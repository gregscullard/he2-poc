const axios = require('axios');
const serverUrl = process.env.VUE_APP_SERVER_URL;
const xApiKey = process.env.VUE_APP_X_API_KEY;

export async function getHotspots() {
    const response = await axios.get(`${serverUrl}/hotspots`);
    return response.data;
}

export async function getHotspotBeaconReports(id) {
    const response = await axios.get(`${serverUrl}/hotspots/beaconReports/${id}`);
    return response.data;
}

export async function addHotspot(name, key) {
    axios.post(`${serverUrl}/hotspots/${name}/${key}`, {}, {
        headers: {
            'x-api-key': xApiKey
        }
    }).catch(error => {
        console.error(error);
    });
}

export function moreOrLessHotspots(more) {
    if (more) {
        axios.post(`${serverUrl}/hotspots`, {}, {
            headers: {
                'x-api-key': xApiKey
            }
        }).catch(error => {
            console.error(error);
        });
    }
}

export function enableHotspot(id) {
    axios.put(`${serverUrl}/hotspots/${id}/enable`,{}, {
        headers: {
            'x-api-key': xApiKey
        }
    }).catch(error => {
        console.error(error);
    });
}

export function disableHotspot(id) {
    axios.put(`${serverUrl}/hotspots/${id}/disable`,{}, {
        headers: {
            'x-api-key': xApiKey
        }
    }).catch(error => {
        console.error(error);
    });
}

export async function apiSetReportInterval(id, interval) {
    axios.put(`${serverUrl}/hotspots/${id}/interval/${interval}`,{}, {
        headers: {
            'x-api-key': xApiKey
        }
    }).catch(error => {
        console.error(error);
    });
}
export async function apiSetReportIntervalAll(interval) {
    axios.post(`${serverUrl}/hotspots/interval/${interval}`,{}, {
        headers: {
            'x-api-key': xApiKey
        }
    }).catch(error => {
        console.error(error);
    });
}
export async function apiSetMinBeacons() {
    // minBeacons: 2,
}
export async function apiSetMinWitness() {
    // minWitness: 1,
}
export async function apiSetEpochSeconds() {
    // epochSeconds: 10
}
