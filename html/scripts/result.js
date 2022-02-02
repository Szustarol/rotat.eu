
fetchResult()

const toBase64 = file => new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = () => resolve(reader.result)
    reader.onerror = error => reject(error)
})

async function fetchResult(){
    try{
        const urlParams = new URLSearchParams(window.location.search)
        const accessKey = urlParams.get('requestCode')
        const targetUrl = apiUrl + "getResult/" + accessKey

        const response = await fetch(targetUrl, {
            method: 'GET',
            mode: 'cors'
        })


        if(!response.ok){
            handleInvalidResponse(response)
        }
        else{
            result = await response.formData().then(
                data => {
                    return data
                }
            )
            .catch(e => {
                showError("Unable to parse server's response.")
                console.log(e)
            })

            const targets = ['result-left-img', 'result-right-img', 'result-back-img']
            const sources = ['left', 'right', 'back']
            console.log(result)
            for(i = 0; i < targets.length; i++){
                document.getElementById(targets[i]).src=
                    await toBase64(result.get(sources[i]))
            }
        }
    }
    catch(error){
        showError("Unable to communicate with the API server.")
        console.log(error)
        return
    }
}