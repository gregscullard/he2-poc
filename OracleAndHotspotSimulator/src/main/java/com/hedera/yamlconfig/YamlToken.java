package com.hedera.yamlconfig;

public class YamlToken {
    private String tokenId = "";
    private String nftTokenId = "";
    private String treasuryAccount = "";
    private String treasuryAccountKey = "";

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getNftTokenId() {
        return this.nftTokenId;
    }
    public void setNftTokenId(String tokenId) {
        this.nftTokenId = tokenId;
    }

    public String getTreasuryAccount() {
        return treasuryAccount;
    }

    public void setTreasuryAccount(String treasuryAccount) {
        this.treasuryAccount = treasuryAccount;
    }

    public String getTreasuryAccountKey() {
        return treasuryAccountKey;
    }

    public void setTreasuryAccountKey(String treasuryAccountKey) {
        this.treasuryAccountKey = treasuryAccountKey;
    }
}