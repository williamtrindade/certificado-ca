package br.ufsm.poli.csi.tapw.certif;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.text.SimpleDateFormat;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws IOException, ParseException, NoSuchAlgorithmException, ClassNotFoundException, InvalidKeySpecException {
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
         *                    TRANSFORMAR PARA JSON
         * ---------------------------------------------------------
         */
        byte[] certificadoByteArray = mapper.writeValueAsBytes(certificadoObject);


        /*
         * ---------------------------------------------------------
         *                    CONECTAR NO SERVIDOR
         * ---------------------------------------------------------
         */
        try (Socket socket = new Socket("192.168.83.1", 8080)) {
            Requisicao requisicao = new Requisicao();
            requisicao.setTipoRequisicao(Requisicao.TipoRequisicao.OBTER_CHAVE_PUBLICA);

            // enviar requisicao
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeObject(requisicao);

            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            Requisicao requisicao1 = (Requisicao) inputStream.readObject();
            byte[] chavePublicaServidor = requisicao1.getResposta();
            // tranformar em public key

            X509EncodedKeySpec spec = new X509EncodedKeySpec(chavePublicaServidor);
            KeyFactory kf = KeyFactory.getInstance("RSA"); // mesmo algoritmo usado na geração
            PublicKey publicKey = kf.generatePublic(spec);



        }

        // Serializar para JSON
        String json = mapper.writeValueAsString(requisicao);
        byte[] data = json.getBytes();


        // Converter para JSON e byte array o certificado
        // Criptografar o certificado com uma chave AES de sessao
        // Criptografar a chave AES de sessao com a chave publica do servidor

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
}