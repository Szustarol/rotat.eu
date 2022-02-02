
function handleInvalidResponse(response){
    switch(response.status){
        case 404:
            showError("API not reachable")
            break;
        case 400:
            response.json().then(
                (data) => {
                    showError("An error occured: " + data.responseContent)
                    console.log(data.responseContent)
                }
            )
            break;
        default:
            showError(`Request returned unknown error ${response.status}`)
    }
}

function showError(message){
    info_placeholder = document.getElementById('infoPlaceholder')

    info_placeholder.innerHTML = `
        <div class="alert alert-warning alert-dismissible" role="alert">
            <span>${message}</span>
            <span type="button" class="btn-close close" data-bs-dismiss="alert" aria-label="Close">
            </span>
        </div>
    `
}

function showInfo(message){
    info_placeholder = document.getElementById('infoPlaceholder')

    info_placeholder.innerHTML = `
        <div class="alert alert-info alert-dismissible" role="alert">
            <span>${message}</span>
            <span type="button" class="btn-close close" data-bs-dismiss="alert" aria-label="Close">
            </span>
        </div>
    `
}