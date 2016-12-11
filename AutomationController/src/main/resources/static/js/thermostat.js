/**
 * Created by Christian Everett on 8/3/2016.
 */
$(document).ready(function ()
{
    var device = "thermostat1";

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

    (function ()
    {
        GET_STATES(refreshPage);

        //Fan, AC, Heat, Off
        fanButton.button().click(modeSelect);
        acButton.button().click(modeSelect);
        heatButton.button().click(modeSelect);
        offButton.button().click(modeSelect);

        //Tempature - Up, Down
        upButton.button().click(tempatureSelect);
        downButton.button().click(tempatureSelect);

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
                POST_ACTION(device, "target_mode=" + fan_mode + "&target_temp=" + tempDisplayDiv.html().slice(0, -1), update);
                break;
            case acButton.attr("id"):
                POST_ACTION(device, "target_mode=" + cool_mode + "&target_temp=" + tempDisplayDiv.html().slice(0, -1), update);
                break;
            case heatButton.attr("id"):
                POST_ACTION(device, "target_mode=" + heat_mode + "&target_temp=" + tempDisplayDiv.html().slice(0, -1), update);
                break;
            case offButton.attr("id"):
                POST_ACTION(device, "target_mode=" + off_mode + "&target_temp=" + tempDisplayDiv.html().slice(0, -1), update);
                break;
            default:
        }
    }

    function tempatureSelect(event)
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
            else
            {
                thermostatModeDiv.html("Off");
                offButton.css("background-color", "rgba(204, 204, 204, 1)");
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
                var dataArray = result[index].data.split("&");

                for (var x = 0; x < dataArray.length; x++)
                {
                    var keyValuePair = dataArray[x].split("=");

                    if (keyValuePair[0] == "target_mode" && !dontChangeTargetUI)
                    {
                        unSelectModeButtons();

                        switch (keyValuePair[1])
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
                    }
                    else if (keyValuePair[0] == "temp")
                    {
                        thermostatTempDiv.html(keyValuePair[1] + "&#x2109");
                    }
                    else if (keyValuePair[0] == "target_temp" && !dontChangeTargetUI)
                    {
                        tempDisplayDiv.html(keyValuePair[1] + "&#x2109");
                    }
                    else if (keyValuePair[0] == "humidity")
                    {
                        thermostatHumidityDiv.html(keyValuePair[1] + "%");
                    }
                    else if (keyValuePair[0] == "location_temp")
                    {
                        outsideTempDiv.html(keyValuePair[1] + "&#x2109");
                    }
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