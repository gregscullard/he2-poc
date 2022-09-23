package com.hedera.he2poc.common.yamlconfig;

import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;

public class YamlHotspot {
    private String accountId = "";
    private String privateKey;
    private int id = 0;
    private String name = "";
    private List<String> paidAccounts = new ArrayList<>();
    private String nft = "";
    public YamlHotspot(int id, String name, String accountId, String privateKey, List<String> paidAccounts, String nft) {
        this.id = id;
        this.name = name;
        this.accountId = accountId;
        this.privateKey = privateKey;
        this.paidAccounts = paidAccounts;
        this.nft = nft;
    }
    public YamlHotspot() {
    }

    public String getNft() {
        return this.nft;
    }

    public void setNft(String nft) {
        this.nft = nft;
    }

    public List<String> getPaidAccounts() {
        return paidAccounts;
    }

    public void setPaidAccounts(List<String> paidAccounts) {
        this.paidAccounts = paidAccounts;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
