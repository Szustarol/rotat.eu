updatePosition()

async function updatePosition(){
    try{
        const urlParams = new URLSearchParams(window.location.search)
        const accessKey = urlParams.get('requestCode')
        const targetUrl = apiUrl + "queue/" + accessKey

        const response = await fetch(targetUrl, {
            method: 'GET',
            mode: 'cors'
        })

        if(!response.ok){
            handleInvalidResponse(response)
        }
        else{
            response.json().then(
                (data) => {
                    queuePosition = parseInt(data.responseContent)
                    if(queuePosition === 0){
                        window.location.href = "result.html?requestCode=" + data.requestCode
                    }
                    document.getElementById("queuePosition").innerHTML = data.responseContent
                }
            )
            setTimeout(updatePosition, 3000)
        }
    }  
    catch (error){
        showError("Unable to communicate with the API server.")
        console.log(error)
        return
    }
}