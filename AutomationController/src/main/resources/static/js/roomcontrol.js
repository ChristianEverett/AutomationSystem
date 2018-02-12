/**
 * Created by Christian Everett on 8/7/2016.
 */

$(document).ready(function ()
{
    var lockDevice = "outlet10";
    var led1Device = "led1";
    var led2Device = "led2";
    var temp_sensorDevice = "temp_sensor1";
    var weatherSensor1 = "weather_sensor1";

    var lockButton = $("#comp-iqsu3uculink");
    var unlockButton = $("#comp-ip1tqeyllink");

    var unlockImg = $("#comp-irjh4j8timgimage");
    var lockImg = $("#comp-iro86zrvimgimage");

    var outsideTempDiv = $("#comp-irjh302u1inlineContent");
    var roomTempDiv = $("#comp-irjh1z0l1bg");

    var led1Slider = $("#comp-irkzi7ulinlineContent").spectrum({
        preferredFormat: "hex",
        move: onColorSliderChange,
        flat: true,
        showInput: true
    });

    var led2Slider = $("#comp-irlctnfxinlineContent").spectrum({
        preferredFormat: "hex",
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
        unlockButton.button().click(onLockUnlockButtonClick);
        lockButton.button().click(onLockUnlockButtonClick);

        for(var x = 0; x < outletSwitchArray.length; x++)
        {
            outletSwitchArray[x].ON.button().click(onOutletButtonClick);
            outletSwitchArray[x].OFF.button().click(onOutletButtonClick);
        }

        connect(function()
        {
            GET_STATES(refreshPage);
            register(lockDevice, updateLock);
            register(led1Device, updateLed);
            register(led2Device, updateLed);
            register(temp_sensorDevice, updateTempSensor);
            register(weatherSensor1, updateWeatherSensor);

            register("outlet1", updateOutlet);
            register("outlet2", updateOutlet);
            register("outlet3", updateOutlet);
            register("outlet4", updateOutlet);
            register("outlet5", updateOutlet);
            register("outlet6", updateOutlet);
            register("outlet7", updateOutlet);
            register("outlet8", updateOutlet);
            register("outlet9", updateOutlet);
            register("outlet10", updateOutlet);
        });
    })();

    function updateLock(json)
    {
        var switchOn = json.params.on;
        setLockImg(switchOn, unlockButton, lockButton, unlockImg, lockImg);
    }

    function updateLed(json)
    {
        var parsedColorsArray = [];

        parsedColorsArray[0] = json.params.red;
        parsedColorsArray[1] = json.params.green;
        parsedColorsArray[2] = json.params.blue;

        if(json.name == led1Device)
            setRGBSlider(parsedColorsArray, led1Slider);
        else
            setRGBSlider(parsedColorsArray, led2Slider);
    }

    function updateTempSensor(json)
    {
        var temperature = json.params.temperature;
        roomTempDiv.html(temperature + "&#x2109");
    }

    function updateWeatherSensor(json)
    {
        var temperature = json.params.temperature;
        outsideTempDiv.html(temperature + "&#x2109");
    }

    function updateOutlet(json)
    {
        checkOutlet(json.name, json.params.on, outletSwitchArray);
    }

    function onLockUnlockButtonClick(event)
    {
        var params = {};
        
        if($(this).attr("id") == unlockButton.attr("id"))
        {
            params["on"] = false;
            POST_ACTION(lockDevice, params, actionCallback);
        }
        else
        {
            params["on"] = true;
            POST_ACTION(lockDevice, params, actionCallback);
        }
    }

    function onColorSliderChange(color)
    {
        var params = {};
        var colorObject = color.toRgb();

        params["red"] = colorObject.r;
        params["green"] = colorObject.g;
        params["blue"] = colorObject.b;

        if($(this).attr("id") == led1Slider.attr("id"))
        {
            POST_ACTION(led1Device, params, actionCallback);
        }
        else
        {
            POST_ACTION(led2Device, params, actionCallback);
        }
    }

    function onOutletButtonClick(event)
    {
        for(var x = 0; x < outletSwitchArray.length; x++)
            if(outletSwitchArray[x].ON.attr("id") == this.id || outletSwitchArray[x].OFF.attr("id") == this.id)
            {
                var params = {};
                params["on"] = (outletSwitchArray[x].ON.attr("id") == this.id);
                POST_ACTION("outlet" + (x + 1), params, actionCallback);
            }
    }

    function refreshPage(result, status, xhr)
    {
        if(xhr.status == 200)
        {
            for(var x = 0; x < result.length; x++)
            {
                switch (result[x].name)
                {
                    case temp_sensorDevice:
                        updateTempSensor(result[x]);
                        break;
                    case weatherSensor1:
                        updateWeatherSensor(result[x]);
                        break;
                    case led1Device:
                    case led2Device:
                        updateLed(result[x]);
                        break;
                    case lockDevice:
                        updateLock(result[x]);
                        break;
                    default:
                }

                if(result[x].type == "outlet")
                    updateOutlet(result[x]);
            }
        }
        else
        {
            alert("Page not working");
        }
    }

    function actionCallback(result, status, xhr)
    {
    }
});