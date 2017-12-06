

function setRGBSlider(rgbValues, slider)
{
    var color = {r: rgbValues[0], g: rgbValues[1], b: rgbValues[2]};

    slider.spectrum("set", color);
}

function setLockImg(setLock, unlockButton, lockButton, unlockImg, lockImg)
{
    lockButton.css("background-color", "rgba(114, 114, 114, 1)");
    unlockButton.css("background-color", "rgba(114, 114, 114, 1)");

    if(setLock)
    {
        unlockImg.hide();
        lockImg.show();
        lockButton.css("background-color", "rgba(204, 204, 204, 1)");
    }
    else
    {
        lockImg.hide();
        unlockImg.show();
        unlockButton.css("background-color", "rgba(204, 204, 204, 1)");
    }
}

function checkOutlet(name, on, outletSwitchArray)
{
    var deviceNumber = parseInt(name.substring(6, name.length)) - 1;

    if(on == "true" || on === true)
    {
        outletSwitchArray[deviceNumber].ON.css("background-color", "rgba(204, 204, 204, 1)");
        outletSwitchArray[deviceNumber].OFF.css("background-color", "rgba(114, 114, 114, 1)");
    }
    else
    {
        outletSwitchArray[deviceNumber].OFF.css("background-color", "rgba(204, 204, 204, 1)");
        outletSwitchArray[deviceNumber].ON.css("background-color", "rgba(114, 114, 114, 1)");
    }
}
