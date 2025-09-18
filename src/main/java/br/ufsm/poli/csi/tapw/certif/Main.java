package br.ufsm.poli.csi.tapw.certif;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javax.crypto.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Main {
    public static void main(String[] args) throws Exception {
        PublicKey publicKey = null;
        Certificado certificadoAssinadoPeloServidor = null;

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
        String jsonCertificate = mapper.writeValueAsString(certificadoObject);
        System.out.println(jsonCertificate);


        /*
         * ---------------------------------------------------------
         *                    CONECTAR NO SERVIDOR
         * ---------------------------------------------------------
         */
        try (Socket socket = new Socket("192.168.83.1", 8080)) {
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

        try (Socket socket = new Socket("192.168.83.1", 8080)) {
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
            certificadoAssinadoPeloServidor = mapper1.readValue(certificadoResposta, Certificado.class);
        }

        System.out.println(certificadoAssinadoPeloServidor);


        // Mensagem mensagem = new Mensagem();
        // mensagem.setMensagem();

    }

    private static Certificado getCertificadoObject(KeyPair keyPair) throws ParseException {
        Certificado certificadoObject = new Certificado();
        certificadoObject.setChavePublica(keyPair.getPublic().getEncoded());
        certificadoObject.setNome("WilliamJordan");
        certificadoObject.setIpOrigem("172.21.25.150");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        certificadoObject.setValidadeInicio(simpleDateFormat.parse("01/08/2025 00:00:00"));
        certificadoObject.setValidadeFim(simpleDateFormat.parse("01/12/2026 00:00:00"));
        return certificadoObject;
    }

    public static byte[] encryptAESKeyWithRSA(SecretKey aesKey, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(aesKey.getEncoded());
    }
}