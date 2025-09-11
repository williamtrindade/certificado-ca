package br.ufsm.poli.csi.tapw.certif;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Date;

@JsonPropertyOrder({
    "chavePublica",
    "nome",
    "ipOrigem",
    "validadeInicio",
    "validadeFim",
    "assinatura",
    "certificadoCA"
})
public class Certificado {
    private byte[] chavePublica;
    private String nome;
    private String ipOrigem;
    private Date validadeInicio;
    private Date validadeFim;
    private byte[] assinatura;
    private Certificado certificadoCA;

    public byte[] getChavePublica() {
        return chavePublica;
    }

    public void setChavePublica(byte[] chavePublic) {
        this.chavePublica = chavePublic;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getIpOrigem() {
        return ipOrigem;
    }

    public void setIpOrigem(String ipOrigem) {
        this.ipOrigem = ipOrigem;
    }

    public Date getValidadeInicio() {
        return validadeInicio;
    }

    public void setValidadeInicio(Date validadeInicio) {
        this.validadeInicio = validadeInicio;
    }

    public Date getValidadeFim() {
        return validadeFim;
    }

    public void setValidadeFim(Date validadeFim) {
        this.validadeFim = validadeFim;
    }

    public byte[] getAssinatura() {
        return assinatura;
    }

    public void setAssinatura(byte[] assinatura) {
        this.assinatura = assinatura;
    }

    public Certificado getCertificadoCA() {
        return certificadoCA;
    }

    public void setCertificadoCA(Certificado certificadoCA) {
        this.certificadoCA = certificadoCA;
    }
}
