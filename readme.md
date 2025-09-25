# Certificate Management and Secure Messaging Project

# Passo a Passo em PT-br

🔑 1. Geração de identidade + certificado

Você cria chave pública/privada e manda pro servidor assinar.

Autenticidade: o servidor atesta que sua chave pública é realmente sua (assinando seu certificado).

🏦 2. Comunicação cliente ↔ servidor (uso de AES + RSA)

Você cifra os dados do certificado com AES e cifra a chave AES com RSA (publica do servidor).

Só o servidor consegue abrir a chave AES e depois os dados.

Confidencialidade: só o servidor tem a chave privada capaz de abrir o conteúdo.

📩 3. Mensagem assinada pelo cliente

Você calcula o hash da mensagem e assina com sua chave privada.

O servidor valida com sua chave pública (do certificado assinado).

Integridade: se a mensagem for alterada, o hash não bate.

Autenticidade: só você (que tem a privada) poderia gerar essa assinatura.

✅ 4. Verificação no servidor

O servidor checa:

se o certificado veio assinado por ele mesmo → autenticidade do cliente.

se a assinatura da mensagem bate com a pública do certificado → integridade + autenticidade.

como tudo trafegou cifrado (AES+RSA) → confidencialidade.

⚡ Em resumo no seu código:

Autenticidade → assinatura do certificado pelo servidor + assinatura da mensagem pelo cliente.

Integridade → uso do hash (SHA-256) assinado, detecta qualquer alteração.

Confidencialidade → criptografia híbrida (AES para os dados, RSA para proteger a chave AES).

___

# Details In English

## Overview
This Java project implements a client-side application for generating, signing, and sending secure messages using certificates and cryptographic techniques. It communicates with a server to obtain a public key, sign certificates, and send encrypted messages. The project uses RSA for asymmetric encryption, AES for symmetric encryption, and SHA-256 for hashing to ensure secure communication.

## Project Structure
The project consists of the following main Java classes:

- **Certificado.java**: Represents a digital certificate containing fields like public key, name, IP origin, validity dates, signature, and a reference to the Certificate Authority (CA) certificate. It uses Jackson annotations for JSON serialization.
- **Requisicao.java**: Defines a request object used for communication with the server. It includes fields for request type, request data, response, and session key, along with an enum for request types (`ASSINAR_CERTIFICADO`, `OBTER_CHAVE_PUBLICA`, `ENVIAR_MENSAGEM`).
- **Mensagem.java**: Represents a message object that includes the message content, associated certificate, and digital signature.
- **Main.java**: Contains the main logic for generating a key pair, creating a certificate, obtaining the server's public key, signing the certificate, and sending a signed message to the server.

## Dependencies
- **Java Cryptography Architecture (JCA)**: For RSA and AES encryption, key generation, and SHA-256 hashing.
- **Jackson Databind**: For JSON serialization and deserialization of objects.
- **Java Standard Library**: For networking (Socket, ObjectInputStream, ObjectOutputStream) and date handling.

## How It Works
1. **Certificate Generation**:
    - A 2048-bit RSA key pair is generated.
    - A `Certificado` object is created with the public key, a name ("WilliamJordan"), IP origin ("127.0.0.1"), and validity dates (Jan 1, 2025 to Dec 31, 2025).

2. **Server Communication**:
    - The client connects to a server at `localhost:8080`.
    - It sends a request to obtain the server's public key (`OBTER_CHAVE_PUBLICA`).
    - The server's public key is received and reconstructed using `X509EncodedKeySpec`.

3. **Certificate Signing**:
    - A 256-bit AES session key is generated.
    - The certificate is serialized to JSON and encrypted with the AES session key.
    - The AES session key is encrypted with the server's RSA public key.
    - A request (`ASSINAR_CERTIFICADO`) is sent to the server with the encrypted certificate and session key.
    - The server returns the signed certificate, which is decrypted using the AES session key and deserialized back into a `Certificado` object.

4. **Message Sending**:
    - A message ("OLA MUNDO") is created and associated with the signed certificate.
    - The message is serialized to JSON, hashed using SHA-256, and the hash is encrypted with the client's RSA private key to create a digital signature.
    - The message, certificate, and signature are sent to the server in a request (`ENVIAR_MENSAGEM`).
    - The server's response is received and printed.

## Usage
1. **Prerequisites**:
    - Ensure Java 8 or higher is installed.
    - The server must be running at `localhost:8080` and support the defined request types.
    - Maven or another build tool is recommended to manage dependencies (Jackson Databind).

2. **Running the Application**:
    - Compile and run `Main.java`.
    - The client will:
        - Generate a certificate.
        - Request the server's public key.
        - Send the certificate for signing.
        - Send a signed "OLA MUNDO" message to the server.
    - The server's response will be printed to the console.

3. **Example Command**:
   ```bash
   javac -cp .:jackson-databind.jar br/ufsm/poli/csi/tapw/certif/*.java
   java -cp .:jackson-databind.jar br.ufsm.poli.csi.tapw.certif.Main
   ```

## Notes
- The server implementation is not included in this project and must be provided separately.
- Ensure the server is configured to handle the `Requisicao` types and return appropriate responses.
- The project assumes the server is running on `localhost:8080`. Modify the `Socket` connection details in `Main.java` if the server runs on a different host or port.
- Error handling is minimal; consider adding robust exception handling for production use.
- The validity dates in the certificate are hardcoded for 2025. Adjust as needed.

## Future Improvements
- Add configuration for server host and port.
- Implement retry logic for failed server connections.
- Enhance security with additional validation of certificates and signatures.
- Add support for multiple messages or concurrent client connections.
