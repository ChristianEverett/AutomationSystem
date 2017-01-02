/**
 * Created by Christian Everett on 8/7/2016.
 */

$(document).ready(function ()
{
    var lockDevice = "switch2";
    var led1Device = "led1";
    var led2Device = "led2";
    var temp_sensorDevice = "temp_sensor1";
    var weatherSensor1 = "weather_sensor1";

    var lockButton = $("#comp-iqsu3ucu");
    var unlockButton = $("#comp-ip1tqeyl");

    var unlockImg = $("#comp-irjh4j8timgimage");
    var lockImg = $("#comp-iro86zrvimgimage");

    var outsideTempDiv = $("#comp-irjh302u1inlineContent");
    var roomTempDiv = $("#comp-irjh1z0l1bg");

    var rgbSliderDiv = $("#comp-irkzi7ulinlineContent");

    var led1Slider = $("#comp-irkzi7ulinlineContent").spectrum({
        move: onColorSliderChange,
        flat: true,
        showInput: true
    });

    var led2Slider = $("#comp-irlctnfxinlineContent").spectrum({
        move: onColorSliderChange,
        flat: true,
        showInput: true
    });

    var outletSwitchArray = [
        {ON: $("#comp-irkzl60elink"), OFF: $("#comp-irkzl60clink")},
        {ON: $("#comp-irkznzx2link"), OFF: $("#comp-irkznzx0link")},
        {ON: $("#comp-irkzqsmp1link"), OFF: $("#comp-irkzqsmolink")},
        {ON: $("#comp-irkzqs9b1link"), OFF: $("#comp-irkzqs9alink")},
        {ON: $("#comp-irkzqr1rlink"), OFF: $("#comp-irkzqr1plink")},
        {ON: $("#comp-irkzqrtz2link"), OFF: $("#comp-irkzqrtylink")},
        {ON: $("#comp-irl4umvylink"), OFF: $("#comp-irl4umvwlink")},
        {ON: $("#comp-irl4vrsflink"), OFF: $("#comp-irl4vrsdlink")},
        {ON: $("#comp-irl4vq6klink"), OFF: $("#comp-irl4vq6ilink")},
        {ON: $("#comp-irl4voahlink"), OFF: $("#comp-irl4voaflink")}
    ];

    (function()
    {
        GET_STATES(refreshPage);
        unlockButton.button().click(onLockUnlockButtonClick);
        lockButton.button().click(onLockUnlockButtonClick);

        for(var x = 0; x < outletSwitchArray.length; x++)
        {
            outletSwitchArray[x].ON.button().click(onOutletButtonClick);
            outletSwitchArray[x].OFF.button().click(onOutletButtonClick);
        }

        setInterval(function(){GET_STATES(refreshPage);}, 10000);
    })();

    function onLockUnlockButtonClick(event)
    {
        if($(this).attr("id") == unlockButton.attr("id"))
        {
            POST_ACTION(lockDevice, "false", actionCallback);
        }
        else
        {
            POST_ACTION(lockDevice, "true", actionCallback);
        }
    }

    function onColorSliderChange(color)
    {
        var colorObject = color.toRgb();

        if($(this).attr("id") == led1Slider.attr("id"))
        {
            POST_ACTION(led1Device, "red=" + colorObject.r + "&green=" + colorObject.g + "&blue=" + colorObject.b, actionCallback);
        }
        else
        {
            POST_ACTION(led2Device, "red=" + colorObject.r + "&green=" + colorObject.g + "&blue=" + colorObject.b, actionCallback);
        }
    }

    function onOutletButtonClick(event)
    {
        for(var x = 0; x < outletSwitchArray.length; x++)
            if(outletSwitchArray[x].ON.attr("id") == this.id || outletSwitchArray[x].OFF.attr("id") == this.id)
                POST_ACTION("outlet" + (x + 1), (outletSwitchArray[x].ON.attr("id") == this.id), actionCallback);
    }

    function checkOutlet(name, isOn)
    {
        deviceNumber = parseInt(name.substring(6, name.length)) - 1;

        if(isOn == "true" || isOn === true)
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

    function setLockImg(setLock)
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

    function setRGBSlider(rgbValues, slider)
    {
        var color = {r: rgbValues[0], g: rgbValues[1], b: rgbValues[2]};

        slider.spectrum("set", color);
    }

    function refreshPage(result, status, xhr)
    {
        if(xhr.status == 200)
        {
            for(var x = 0; x < result.length; x++)
            {
                switch (result[x].deviceName)
                {
                    case temp_sensorDevice:
                        roomTempDiv.html(result[x].temperature + "&#x2109");
                        break;
                    case weatherSensor1:
                        outsideTempDiv.html(result[x].temperature + "&#x2109");
                        break;
                    case led1Device:
                    case led2Device:
                        var parsedColorsArray = [];

                        parsedColorsArray[0] = result[x].red;
                        parsedColorsArray[1] = result[x].green;
                        parsedColorsArray[2] = result[x].blue;

                        if(result[x].deviceName == led1Device)
                            setRGBSlider(parsedColorsArray, led1Slider);
                        else
                            setRGBSlider(parsedColorsArray, led2Slider);

                        break;
                    case lockDevice:
                        setLockImg(result[x].deviceOn);
                        break;
                    default:
                        if(result[x].deviceName.indexOf("outlet") !== -1)
                            checkOutlet(result[x].deviceName, result[x].deviceOn);
                }
            }
        }
        else
        {
            alert("Page not working");
        }
    }

    function actionCallback(result, status, xhr)
    {
        if(xhr.status == 200)
        {
            var json = jQuery.parseJSON(this.data);

            if (json.device == lockDevice)
            {
                if(json.data == "true")
                {
                    setLockImg(true);
                }
                else
                {
                    setLockImg(false);
                }
            }
            else if(json.device.indexOf("outlet") !== -1)
            {
                checkOutlet(json.device, json.data);
            }
        }
        else
        {
            alert("Page not working");
        }
    }
});