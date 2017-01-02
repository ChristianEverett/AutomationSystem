/**
 * Created by Christian Everett on 8/3/2016.
 */
$(document).ready(function ()
{
    var thermostat_device = "thermostat1";
    var temperature_device = "temp_sensor2";
    var weather_sensor = "weather_sensor1";

    var thermostatTempDiv = $("#comp-irhyydxlinlineContent");
    var thermostatHumidityDiv = $("#comp-irpookur1inlineContent");
    var thermostatModeDiv = $("#comp-irhyyw5iinlineContent");
    var outsideTempDiv = $("#comp-irxjq407inlineContent");

    var fanButton = $("#comp-irffwqe4link");
    var acButton = $("#comp-irffwy09link");
    var heatButton = $("#comp-irffx5bzlink");
    var offButton = $("#comp-irfnr972link");

    var downButton = $("#comp-irfg7zhflink");
    var upButton = $("#comp-irfg7urclink");

    var tempDisplayDiv = $("#comp-irfga970inlineContent");

    var dontChangeTargetUI = false;

    var outletSwitchArray = [
        {ON: $("#comp-ix6pzzx0link"), OFF: $("#comp-ix6pzzwwlink")},
        {ON: $("#comp-ix6pzzx6link"), OFF: $("#comp-ix6pzzx4link")},
        {ON: $("#comp-ix6pzzxblink"), OFF: $("#comp-ix6pzzx9link")},
        {ON: $("#comp-ix6pzzxe3link"), OFF: $("#comp-ix6pzzxd1link")},
        {ON: $("#comp-ix6q2prblink"), OFF: $("#comp-ix6q2pr9link")},
        {ON: $("#comp-irkzqrtz2link"), OFF: $("#comp-irkzqrtylink")}
    ];
    
    (function ()
    {
        GET_STATES(refreshPage);

        //Fan, AC, Heat, Off
        fanButton.button().click(modeSelect);
        acButton.button().click(modeSelect);
        heatButton.button().click(modeSelect);
        offButton.button().click(modeSelect);

        //Temperature - Up, Down
        upButton.button().click(temperatureSelect);
        downButton.button().click(temperatureSelect);

        for(var x = 0; x < outletSwitchArray.length; x++)
        {
            outletSwitchArray[x].ON.button().click(onOutletButtonClick);
            outletSwitchArray[x].OFF.button().click(onOutletButtonClick);
        }

        setInterval(function ()
        {
            GET_STATES(refreshPage);
        }, 10000);
    }());

    function unSelectModeButtons()
    {
        fanButton.css("background-color", "rgba(114, 114, 114, 1)");
        acButton.css("background-color", "rgba(114, 114, 114, 1)");
        heatButton.css("background-color", "rgba(114, 114, 114, 1)");
        offButton.css("background-color", "rgba(114, 114, 114, 1)");
    }

    function modeSelect(event)
    {
        unSelectModeButtons();

        switch (this.id)
        {
            case fanButton.attr("id"):
                POST_ACTION(thermostat_device, "target_mode=" + fan_mode + "&target_temp=" + tempDisplayDiv.html().slice(0, -1), update);
                break;
            case acButton.attr("id"):
                POST_ACTION(thermostat_device, "target_mode=" + cool_mode + "&target_temp=" + tempDisplayDiv.html().slice(0, -1), update);
                break;
            case heatButton.attr("id"):
                POST_ACTION(thermostat_device, "target_mode=" + heat_mode + "&target_temp=" + tempDisplayDiv.html().slice(0, -1), update);
                break;
            case offButton.attr("id"):
                POST_ACTION(thermostat_device, "target_mode=" + off_mode + "&target_temp=" + tempDisplayDiv.html().slice(0, -1), update);
                break;
            default:
        }
    }

    function temperatureSelect(event)
    {
        dontChangeTargetUI = true;
        var split = tempDisplayDiv.html().slice(0, -1);
        unSelectModeButtons();

        if (this.id == upButton.attr("id"))
        {
            split = parseInt(split) + 1;
        }
        else
        {
            split = parseInt(split) - 1;
        }

        tempDisplayDiv.html(split + "&#x2109");
    }

    function onOutletButtonClick(event)
    {
        for(var x = 0; x < outletSwitchArray.length; x++)
            if(outletSwitchArray[x].ON.attr("id") == this.id || outletSwitchArray[x].OFF.attr("id") == this.id)
                POST_ACTION("outlet" + (x + 1), (outletSwitchArray[x].ON.attr("id") == this.id), update);
    }

    function checkOutlet(name, isOn)
    {
        deviceNumber = parseInt(name.substring(6, name.length)) - 1;

        if(deviceNumber > 4)
            return;

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

    function update(result, status, xhr)
    {
        if (xhr.status == 200)
        {
            if (this.data.search(fan_mode) != -1)
            {
                thermostatModeDiv.html("Fan");
                fanButton.css("background-color", "rgba(204, 204, 204, 1)");
            }
            else if (this.data.search(cool_mode) != -1)
            {
                thermostatModeDiv.html("AC");
                acButton.css("background-color", "rgba(204, 204, 204, 1)");
            }
            else if (this.data.search(heat_mode) != -1)
            {
                thermostatModeDiv.html("Heat");
                heatButton.css("background-color", "rgba(204, 204, 204, 1)");
            }
            else if (this.data.search(off_mode) != -1)
            {
                thermostatModeDiv.html("Off");
                offButton.css("background-color", "rgba(204, 204, 204, 1)");
            }
            else
            {
                var json = jQuery.parseJSON(this.data);
                checkOutlet(json.device, json.data);
            }
        }

        dontChangeTargetUI = false;
    }

    function refreshPage(result, status, xhr)
    {
        if (xhr.status == 200)
        {
            for (var index = 0; index < result.length; index++)
            {
                if(result[index].deviceName == thermostat_device)
                {
                    unSelectModeButtons();

                    switch (result[index].targetMode)
                    {
                        case off_mode:
                            thermostatModeDiv.html("Off");
                            offButton.css("background-color", "rgba(204, 204, 204, 1)");
                            break;
                        case fan_mode:
                            thermostatModeDiv.html("Fan");
                            fanButton.css("background-color", "rgba(204, 204, 204, 1)");
                            break;
                        case cool_mode:
                            thermostatModeDiv.html("AC");
                            acButton.css("background-color", "rgba(204, 204, 204, 1)");
                            break;
                        case heat_mode:
                            thermostatModeDiv.html("Heat");
                            heatButton.css("background-color", "rgba(204, 204, 204, 1)");
                            break;
                        default:
                    }

                    if(!dontChangeTargetUI)
                        tempDisplayDiv.html(result[index].targetTemperature + "&#x2109");
                }
                else if(result[index].deviceName == temperature_device)
                {
                    thermostatTempDiv.html(result[index].temperature + "&#x2109");
                    thermostatHumidityDiv.html(result[index].humidity + "%");
                }
                else if(result[index].deviceName == weather_sensor)
                {
                    outsideTempDiv.html(result[index].temperature + "&#x2109");
                }
                else
                {
                    if(result[index].deviceName.indexOf("outlet") !== -1)
                        checkOutlet(result[index].deviceName, result[index].deviceOn);
                }
            }
        }
        else
        {
            alert("Page not working, contact support: Christian.everett1@gmail.com");
        }

        dontChangeTargetUI = false;
    }

    var off_mode = "off_mode";
    var fan_mode = "fan_mode";
    var cool_mode = "cool_mode";
    var heat_mode = "heat_mode";
});