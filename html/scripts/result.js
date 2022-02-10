
const toBase64 = file => new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = () => resolve(reader.result)
    reader.onerror = error => reject(error)
})

const sources = ['left', 'right', 'back']
const targets = ['result-left-img', 'result-right-img', 'result-back-img']

var imagesData = []
var imagesCanvas = []

fetchResult()

function getPixelColor(pixelData, row, col, channel){
    return pixelData[(row*64+col)*4+channel]
}

function setPixelColor(pixelData, row, col, channel, color){
    pixelData[(row*64+col)*4+channel] = color
}

document.querySelector("#pixelizer").addEventListener("change", (event) => {
    const boxSize = parseInt(event.target.value)
    for(i = 0; i < imagesData.length; i++){
        pixelData = imagesData[i].getImageData(0, 0, 64, 128).data
        for(sh = 0; sh < 128; sh+=boxSize){
            for(sw = 0; sw < 64; sw += boxSize){
                shend = Math.min(sh+boxSize, 128)
                swend = Math.min(sw+boxSize, 64)

                boxRedPixels = []
                boxBluePixels = []
                boxGreenPixels = []

                for(h = sh; h < shend; h++){
                    for(w = sw; w < swend; w++){
                        boxRedPixels.push(getPixelColor(pixelData, h, w, 0))
                        boxGreenPixels.push(getPixelColor(pixelData, h, w, 1))
                        boxBluePixels.push(getPixelColor(pixelData, h, w, 2))
                    }
                }

                boxRedAvg = boxRedPixels.reduce((a, b) => a + b, 0) / boxRedPixels.length
                boxBlueAvg = boxBluePixels.reduce((a, b) => a + b, 0) / boxBluePixels.length
                boxGreenAvg = boxGreenPixels.reduce((a, b) => a + b, 0) / boxGreenPixels.length


                for(h = sh; h < shend; h++){
                    for(w = sw; w < swend; w++){
                        setPixelColor(pixelData, h, w, 0, boxRedAvg)
                        setPixelColor(pixelData, h, w, 1, boxGreenAvg)
                        setPixelColor(pixelData, h, w, 2, boxBlueAvg)
                    }
                }
            }
        }
        dataBackup = imagesData[i].getImageData(0, 0, 64, 128)
        imagesData[i].putImageData(new ImageData(pixelData, 64), 0, 0)
        const baseUrl = imagesCanvas[i].toDataURL()
        imagesData[i].putImageData(dataBackup, 0, 0)
        document.getElementById(targets[i]).src = baseUrl       
    }
})

async function fetchResult(){
    try{
        const urlParams = new URLSearchParams(window.location.search)
        const accessKey = urlParams.get('requestCode')
        

        for(i = 0; i < sources.length; i++){
            const source = sources[i]
            const targetUrl = apiUrl + "getResult/" + accessKey + '/' + source

            const responseBlob = await fetch(targetUrl, {
                method: 'GET',
                mode: 'cors'
            }).then(response => {
                if (!response.ok){
                    handleInvalidResponse(response)
                    return null
                }
                else{
                    return response.blob()
                }
            })

            if(responseBlob == null)
                return

            const imageData = await toBase64(responseBlob)

            document.getElementById(targets[i]).src = imageData
            canvas = document.createElement("canvas")
            canvas.width = 64
            canvas.height = 128
            const img = new Image()
            img.src = imageData
            await img.decode()
            var ctx = canvas.getContext("2d")
            ctx.drawImage(img, 0, 0)
            imagesData.push(ctx)
            imagesCanvas.push(canvas)
        }
    }
    catch(error){
        showError("Unable to communicate with the API server.")
        console.log(error)
        return
    }
}
