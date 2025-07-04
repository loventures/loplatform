function doLogin(username, password, remember, mechanism) {
    return de.ajax({
        type: 'POST',
        url: '/api/v2/sessions/login',
        data: { username: username, password: password, remember: remember, mechanism: mechanism },
        headers: { 'X-Interactive': 'true' }
    });
}

function doLogout() {
    return de.ajax({
        type: 'POST',
        url: '/api/v2/sessions/logout'
    });
}

function doExit() {
    return de.ajax({
        type: 'POST',
        url: '/api/v2/sessions/exit'
    });
}
function recoverPassword(username, properties, message, redirect) {
    return de.ajax({
        type: 'POST',
        url: '/api/v2/passwords/recover',
        data: { search: username,
                properties: properties,
                message: message,
                redirect: redirect }
    });
}

function validateResetToken(token) {
    return de.ajax({
        url: '/api/v2/passwords/reset',
        data: { token: token }
    });
}

function resetPassword(token, password) {
    return de.ajax({
        type: 'POST',
        url: '/api/v2/passwords/reset',
        data: { token: token, password: password }
    });
}

function registerRedeem(given, family, email, password, accessCode, accessCodeSchema) {
    var qs = '?accessCode=' + encodeURIComponent(accessCode);
    if (accessCodeSchema) {
        qs = qs + '&accessCodeSchema=' + encodeURIComponent(accessCodeSchema);
    }
    var user = { userName: email,
                 givenName: given,
                 familyName: family,
                 emailAddress: email,
                 password: password };
    return de.ajax({
        type: 'POST',
        url: '/api/v2/accounts/registerRedeem' + qs,
        contentType: 'application/json',
        data: JSON.stringify(user),
        processData: false
    });
}

function requestAccount(given, family, email, password, attributes, redirect) {
    var qs = redirect ? '?redirect=' + encodeURIComponent(redirect) : '';
    var request = {
        user: {
            userName: email,
            givenName: given,
            familyName: family,
            emailAddress: email,
            password: password
        },
        attributes: attributes
    };
    return de.ajax({
        type: 'POST',
        url: '/api/v2/accountRequests' + qs,
        contentType: 'application/json',
        data: JSON.stringify(request),
        processData: false
    });
}
