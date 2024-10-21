/**
 * @typedef {Object} Session
 * @property {string} sessionId
 * @property {string} playerId
 * @property {string} username
 */

/**
 * @returns {Session | undefined}
 */
function getCookie() {
    const value = ('; ' + document.cookie).split(`; sessionId=`).pop().split(';')[0];
    if (value === "") {
        return undefined;
    }
    // session value will be an ESCAPED JSON string literal
    // we need to parse from a string literal into a JSON string, then the JSON string into a JSON object
    return JSON.parse(JSON.parse(value));
}