const axios = require('axios');
const serverUrl = process.env.VUE_APP_SERVER_URL;
const xApiKey = process.env.VUE_APP_X_API_KEY;

export async function getHotspots() {
    const response = await axios.get(serverUrl.concat('/hotspots'));
    return response.data;
}

export async function getHotspotBeaconReports(id) {
    const response = await axios.get(serverUrl.concat('/hotspots/beaconReports/'.concat(id)));
    return response.data;
}

export function moreOrLessHotspots(more) {
    if (more) {
        axios.post(serverUrl.concat('/hotspots'),{}, {
            headers: {
                'x-api-key': xApiKey
            }
        }).catch(error => {
            console.error(error);
        });
    } else {
        axios.delete(serverUrl.concat('/hotspots'), {
            headers: {
                'x-api-key': xApiKey
            }
        }).catch(error => {
            console.error(error);
        });
    }
}
