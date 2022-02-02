fileDropper = document.getElementById('fileDropper')

function isImage(file){
    return file && file['type'].split('/')[0] === 'image'
}

fileDropper.onchange = async e =>{
    if(e.target.files.length != 1){
        showError("Please only select one file.")
        return
    }
    file = e.target.files[0]
    if(!isImage(file)){
        showError("You need to select an image file.")
        return
    }

    if(Image.size / 1024 / 1024 > 1){
        showError("Maximum file size is 1 MB.")
        return 
    }

    const formData = new FormData()
    formData.append('file', file)

    const targetUrl = apiUrl + "enqueue"
    console.log(targetUrl)

    try{
        const response = await fetch(targetUrl, {
            method: 'POST',
            mode: 'cors',
            headers: {
                'Content-Length': file.length,
            },
            body: formData
        })
        

        if (!response.ok){
            handleInvalidResponse(response)
        }
        else{
            showInfo("Image successfully uploaded. Proceeding to the queue")
            response.json().then(
                (data) => {
                    window.location.href = "queue.html?requestCode=" + data.requestCode
                }
            )
        }
    }
    catch{
        showError("Unable to communicate with the API server.")
        return
    }
}