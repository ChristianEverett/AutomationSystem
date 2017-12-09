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
        }, 50000);

        connect(function()
        {
            GET_STATES(refreshPage);
            register(thermostat_device, updateThermostat);
            register(temperature_device, updateTempSensor);
            register(weather_sensor, updateWeatherSensor);

            register("outlet1", updateOutlet);
            register("outlet2", updateOutlet);
            register("outlet3", updateOutlet);
            register("outlet4", updateOutlet);
            register("outlet5", updateOutlet);
        });
    }());

    function updateThermostat(json)
    {
        unSelectModeButtons();

        if(!dontChangeTargetUI)
            switch (json.params.target_mode)
            {
                case off_mode:
                    offButton.css("background-color", "rgba(204, 204, 204, 1)");
                    break;
                case fan_mode:
                    fanButton.css("background-color", "rgba(204, 204, 204, 1)");
                    break;
                case cool_mode:
                    acButton.css("background-color", "rgba(204, 204, 204, 1)");
                    break;
                case heat_mode:
                    heatButton.css("background-color", "rgba(204, 204, 204, 1)");
                    break;
                default:
            }

        switch(json.params.mode)
        {
            case off_mode:
                thermostatModeDiv.html("Off");
                break;
            case fan_mode:
                thermostatModeDiv.html("Fan");
                break;
            case cool_mode:
                thermostatModeDiv.html("AC");
                break;
            case heat_mode:
                thermostatModeDiv.html("Heat");
                break;
            default:
        }

        if(!dontChangeTargetUI)
            tempDisplayDiv.html(json.params.target_temp + "&#x2109");
    }

    function updateTempSensor(json)
    {
        var temperature = json.params.temperature;
        thermostatTempDiv.html(temperature + "&#x2109");
        thermostatHumidityDiv.html(json.params.humidity + "%");
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

    function unSelectModeButtons()
    {
        fanButton.css("background-color", "rgba(114, 114, 114, 1)");
        acButton.css("background-color", "rgba(114, 114, 114, 1)");
        heatButton.css("background-color", "rgba(114, 114, 114, 1)");
        offButton.css("background-color", "rgba(114, 114, 114, 1)");
    }

    function modeSelect(event)
    {
        var params = {};

        unSelectModeButtons();

        switch (this.id)
        {
            case fanButton.attr("id"):
                params["target_mode"] = fan_mode;
                params["target_temp"] = parseInt(tempDisplayDiv.html().slice(0, -1));
                POST_ACTION(thermostat_device, params, update);
                break;
            case acButton.attr("id"):
                params["target_mode"] = cool_mode;
                params["target_temp"] = parseInt(tempDisplayDiv.html().slice(0, -1));
                POST_ACTION(thermostat_device, params, update);
                break;
            case heatButton.attr("id"):
                params["target_mode"] = heat_mode;
                params["target_temp"] = parseInt(tempDisplayDiv.html().slice(0, -1));
                POST_ACTION(thermostat_device, params, update);
                break;
            case offButton.attr("id"):
                params["target_mode"] = off_mode;
                params["target_temp"] = parseInt(tempDisplayDiv.html().slice(0, -1));
                POST_ACTION(thermostat_device, params, update);
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
            {
                var params = {};
                params["on"] = (outletSwitchArray[x].ON.attr("id") == this.id);
                POST_ACTION("outlet" + (x + 1), params, update);
            }
    }

    function update(result, status, xhr)
    {
        if (xhr.status == 200)
        {
            var json = jQuery.parseJSON(this.data);

            if(json.name == thermostat_device)
                switch (json.params.target_mode)
                {
                    case off_mode:
                        offButton.css("background-color", "rgba(204, 204, 204, 1)");
                        break;
                    case fan_mode:
                        fanButton.css("background-color", "rgba(204, 204, 204, 1)");
                        break;
                    case cool_mode:
                        acButton.css("background-color", "rgba(204, 204, 204, 1)");
                        break;
                    case heat_mode:
                        heatButton.css("background-color", "rgba(204, 204, 204, 1)");
                        break;
                    default:
                }
        }

        dontChangeTargetUI = false;
    }

    function refreshPage(result, status, xhr)
    {
        if (xhr.status == 200)
        {
            for (var x = 0; x < result.length; x++)
            {
                switch (result[x].name)
                {
                    case temperature_device:
                        updateTempSensor(result[x]);
                        break;
                    case weather_sensor:
                        updateWeatherSensor(result[x]);
                        break;
                    case thermostat_device:
                        updateThermostat(result[x]);
                        break;
                    default:
                        if(result[x].type == "outlet")
                        {
                            var deviceNumber = parseInt(result[x].name.substring(6, result[x].name.length)) - 1;

                            if(deviceNumber < outletSwitchArray.length)
                                updateOutlet(result[x]);
                        }
                }
            }
        }
        else
        {
            //TODO when cant connect
        }

        dontChangeTargetUI = false;
    }

    var off_mode = "off_mode";
    var fan_mode = "fan_mode";
    var cool_mode = "cool_mode";
    var heat_mode = "heat_mode";
});