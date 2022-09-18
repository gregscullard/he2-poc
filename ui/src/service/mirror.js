const axios = require('axios');
const serverUrl = process.env.VUE_APP_MIRROR_URL;

export async function mirrorGetReports(accountId) {
    const url = `${serverUrl}/transactions?account.id=${accountId}&limit=5&order=desc&transactiontype=CONSENSUSSUBMITMESSAGE`;
    const response = await axios.get(url);
    return response.data;
}
export async function tokenTransfers(accountId) {
    const url = `https://testnet.mirrornode.hedera.com/api/v1/transactions?account.id=${accountId}&limit=5&order=desc&transactiontype=CRYPTOTRANSFER`;
    const response = await axios.get(url);
    return response.data;
}
