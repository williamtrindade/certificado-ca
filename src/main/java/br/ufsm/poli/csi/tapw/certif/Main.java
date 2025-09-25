package br.ufsm.poli.csi.tapw.certif;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javax.crypto.*;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws Exception {
        PublicKey publicKey;
        Certificado certificadoAssinadoPeloServidor;

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        /*
         * ---------------------------------------------------------
         *                     GERAR CERTIFICADO
         * ---------------------------------------------------------
         */
        Certificado certificadoObject = getCertificadoObject(keyPair);

        /*
         * ---------------------------------------------------------
         *                    TRANSFORMAR PARA JSON
         * ---------------------------------------------------------
         */
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);

        /*
         * ---------------------------------------------------------
         *                    CONECTAR NO SERVIDOR
         * ---------------------------------------------------------
         */
        try (Socket socket = new Socket("localhost", 8080)) {
            // Cria uma requisicao de obter chave publica
            Requisicao requisicao = new Requisicao();
            requisicao.setTipoRequisicao(Requisicao.TipoRequisicao.OBTER_CHAVE_PUBLICA);

            // enviar requisicao de obter chave publica
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeObject(requisicao);

            // Receber resposta de obter chave publica
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            Requisicao requisicao1 = (Requisicao) inputStream.readObject();
            byte[] chavePublicaServidor = requisicao1.getResposta();

            // Criar chave publica do servidor
            X509EncodedKeySpec spec = new X509EncodedKeySpec(chavePublicaServidor);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            publicKey = kf.generatePublic(spec);
            // System.out.println("Chave publica do servidor: " + publicKey);
        }

        try (Socket socket = new Socket("localhost", 8080)) {
            // Gerar chave AES de sessao
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey chaveAESSessao = keyGen.generateKey();

            // pegar o byte array do certificado
            byte[] certificadoByteArray = mapper.writeValueAsBytes(certificadoObject);

            // Criptografar o certificado com a chave AES de sessao
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, chaveAESSessao);
            byte[] certificadoCriptografado = cipher.doFinal(certificadoByteArray);

            // criptografar a chave de sessao com a chave publica do servidor
            byte[] chaveDeSessaoCriptografada = encryptAESKeyWithRSA(chaveAESSessao, publicKey);

            /*
             * ENVIAR REQUEST DE ASSINAR CERTIFICADO
             */
            // Criar objeto requisição
            Requisicao requisicao = new Requisicao();
            requisicao.setTipoRequisicao(Requisicao.TipoRequisicao.ASSINAR_CERTIFICADO);
            requisicao.setChaveSessao(chaveDeSessaoCriptografada);
            requisicao.setRequisicao(certificadoCriptografado);

            // Enviar requisição
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeObject(requisicao);

            // Receber resposta
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            Requisicao requisicao1 = (Requisicao) inputStream.readObject();
            byte[] certificadoCriptografadoResposta = requisicao1.getResposta();

            // descriptografar o certificado (resposta)
            Cipher cipher1 = Cipher.getInstance("AES");
            cipher1.init(Cipher.DECRYPT_MODE, chaveAESSessao);
            byte[] certificadoResposta = cipher1.doFinal(certificadoCriptografadoResposta);

            // transformar em objeto
            ObjectMapper mapper1 = new ObjectMapper();
            mapper1.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);

            //String stringResposta = new String(certificadoResposta);
            certificadoAssinadoPeloServidor = mapper1.readValue(certificadoResposta, Certificado.class);
        }

        /*
         * CRIAR UMA MENSAGEM
         */
        // Criar a mensagem
        Mensagem mensagem = new Mensagem();
        mensagem.setMensagem("OLA MUNDO".getBytes());

        // COLOCAR MEU CERTIFICADO
        mensagem.setCertificado(certificadoAssinadoPeloServidor);

        // CONVERTER A MENSAGEM PARA BYTE[]
        ObjectMapper mapper2 = new ObjectMapper();
        mapper2.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);
        byte[] mensagemEmBytes = mapper2.writeValueAsBytes(mensagem);

        // GERAR UM HASH DA MENSAGEM
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] digestBytesMensagem = digest.digest(mensagemEmBytes);

        // CRIPTOGRAFAR O HASH GERADO DA MENSAGEM COM A CHAVE PRIVADA DO CERTIFICADO
        byte[] hashMensagemCriptografado = encryptWithPrivateKey(digestBytesMensagem, keyPair.getPrivate());

        // setar assinatura da memsagem
        mensagem.setAssinatura(hashMensagemCriptografado);

        // converter mensagem para byte[]
        byte[] mensagemComAssinatura = mapper2.writeValueAsBytes(mensagem);

        try (Socket socket = new Socket("localhost", 8080)) {
            /*
             * ENVIAR REQUEST DE ENVIAR MENSAGEM
             */
            // Criar objeto requisição
            Requisicao requisicaoFinal = new Requisicao();
            requisicaoFinal.setTipoRequisicao(Requisicao.TipoRequisicao.ENVIAR_MENSAGEM);
            requisicaoFinal.setRequisicao(mensagemComAssinatura);

            // Enviar requisição
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeObject(requisicaoFinal);

            // Receber resposta
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            Requisicao resposta = (Requisicao) inputStream.readObject();

            byte[] respostaMensagem = resposta.getResposta();
            System.out.println(Arrays.toString(respostaMensagem));
        }
    }

    private static Certificado getCertificadoObject(KeyPair keyPair) throws ParseException {
        Certificado certificadoObject = new Certificado();
        certificadoObject.setChavePublica(keyPair.getPublic().getEncoded());
        certificadoObject.setNome("WilliamJordan");
        certificadoObject.setIpOrigem("127.0.0.1");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        certificadoObject.setValidadeInicio(simpleDateFormat.parse("2025/01/01 00:00:00"));
        certificadoObject.setValidadeFim(simpleDateFormat.parse("2025/12/31 00:00:00"));
        return certificadoObject;
    }

    public static byte[] encryptAESKeyWithRSA(SecretKey aesKey, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(aesKey.getEncoded());
    }

    public  static byte[] encryptWithPrivateKey(byte[] data, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        return cipher.doFinal(data);
    }
}