const axios = require('axios');
const serverUrl = process.env.VUE_APP_SERVER_URL;
const xApiKey = process.env.VUE_APP_X_API_KEY;

export async function apiGetHotspots() {
    const response = await axios.get(`${serverUrl}/hotspots`);
    return response.data;
}

export async function apiGetHotspotBeaconReports(id) {
    const response = await axios.get(`${serverUrl}/hotspots/beaconReports/${id}`);
    return response.data;
}

export async function apiAddHotspot(name, key, nft) {
    axios.post(`${serverUrl}/hotspots/${name}/${key}/${nft}`, {}, {
        headers: {
            'x-api-key': xApiKey
        }
    }).then(() => {
        return "Hotspot added";
    }).catch(error => {
        console.error(error);
        return error;
    });
}

export function apiEnableHotspot(id) {
    axios.put(`${serverUrl}/hotspots/${id}/enable`,{}, {
        headers: {
            'x-api-key': xApiKey
        }
    }).then(() => {
        return "Hotspot enabled";
    }).catch(error => {
        console.error(error);
        return error;
    });
}

export function apiDisableHotspot(id) {
    axios.put(`${serverUrl}/hotspots/${id}/disable`,{}, {
        headers: {
            'x-api-key': xApiKey
        }
    }).then(() => {
        return "Hotspot disabled";
    }).catch(error => {
        console.error(error);
        return error;
    });
}

export async function apiSetReportInterval(id, interval) {
    axios.put(`${serverUrl}/hotspots/${id}/interval/${interval}`,{}, {
        headers: {
            'x-api-key': xApiKey
        }
    }).then(() => {
        return `Report interval set`;
    }).catch(error => {
        console.error(error);
        return error;
    });
}
export async function apiSetReportIntervalAll(interval) {
    axios.post(`${serverUrl}/hotspots/interval/${interval}`,{}, {
        headers: {
            'x-api-key': xApiKey
        }
    }).then(() => {
        return `Report interval set`;
    }).catch(error => {
        console.error(error);
        return error;
    });
}
export async function apiSetMinBeacons(minBeaconReports) {
    axios.post(`${serverUrl}/oracles/?minBeaconReports=${minBeaconReports}`,{}, {
        headers: {
            'x-api-key': xApiKey
        }
    }).then(() => {
        return `Minimum beacon reports set`;
    }).catch(error => {
        console.error(error);
        return error;
    });
}
export async function apiSetMinWitness(minWitnessReports) {
    axios.post(`${serverUrl}/oracles/?minWitnessReports=${minWitnessReports}`,{}, {
        headers: {
            'x-api-key': xApiKey
        }
    }).then(() => {
        return `Minimum witness reports set`;
    }).catch(error => {
        console.error(error);
        return error;
    });
}

export async function apiGetTpsReport() {
    const response = await axios.get(`${serverUrl}/oracles/tps`);
    return response.data;
}

export async function apiSetEpochSeconds(epochDuration) {
    axios.post(`${serverUrl}/oracles/?epochDuration=${epochDuration}`,{}, {
        headers: {
            'x-api-key': xApiKey
        }
    }).then(() => {
        return `Epoch seconds set`;
    }).catch(error => {
        console.error(error);
        return error;
    });
}
