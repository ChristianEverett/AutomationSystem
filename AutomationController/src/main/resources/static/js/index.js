/**
 * Created by Christian Everett on 5/12/2016.
 */



$(document).ready(function ()
{
    //Actions
    var LED2 = "led2";
    var LOCK = "switch4";

    (function ()
    {
        GET_STATES(refreshPage);

        function refreshPage(result, status, xhr)
        {
            for(var x = 0; x < result.length; x++)
            {
                switch (result[x].command)
                {
                    case LED2:
                        var rgbValues = result[x].data.split(" ");
                        var color = {r: rgbValues[0], g:rgbValues[1], b:rgbValues[2]};

                        flat.spectrum("set", color);
                        break;
                    case LOCK:
                        if(result[x].data == "true")
                            lockIndicatorImg.attr("src", "res/locked.png");
                        else
                            lockIndicatorImg.attr("src", "res/unlocked.png");
                        break;
                }
            }

            GET_TIMERS(loadTimers);
        }

        //RGB Selector ---------------------------------------------------------------

        var flat = $("#flat").spectrum({
            move: colorChange,
            flat: true,
            showInput: true
        });

        var flat_selector = $("#flat_selector").spectrum({
            flat: true,
            showInput: true
        });

        function colorChange(color)
        {
            var colorObject = color.toRgb();

            POST_ACTION(LED2, "red=" + colorObject.r + "&green=" + colorObject.g + "&blue=" + colorObject.b);
        }

        //RGB Button Table -------------------------------------------------------------

        var color_slider = $("#color_slider").slider({min: 0, step: .05, max: 1});

        var RGB_table = $("#color_table");

        jQuery.ajax({dataType: "json", url: "res/colors.json", success: generateTable});

        function generateTable(data)
        {
            var table_data = "<tr>";

            for(var x = 0; x < data.colorsArray.length; x++)
            {
                var color = data.colorsArray[x].colorName;
                var hexValue = data.colorsArray[x].hexValue;

                table_data += "<td class='.color_table' id='" + color + "' bgcolor='" + hexValue + "'>" + color + "</td>";

                if((x + 1) % 5 == 0)
                {
                    table_data += "</tr><tr>";
                }
            }

            table_data += "</tr>";
            RGB_table.html(table_data);

            var color_tds = $("#color_table td");
            color_tds.click(colorButtonClick);
        }

        function colorButtonClick(event)
        {
            var hexColor = $(this).attr("bgcolor");
            var rgbColor = hexToRgb(hexColor);

            var r = parseInt(rgbColor.r * color_slider.slider("value"));
            var g = parseInt(rgbColor.g * color_slider.slider("value"));
            var b = parseInt(rgbColor.b * color_slider.slider("value"));

            POST_ACTION(LED2,  "red=" + r + "&green=" + g + "&blue=" +  b);
        }

        function hexToRgb(hex)
        {
            var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
            return result ? {
                r: parseInt(result[1], 16),
                g: parseInt(result[2], 16),
                b: parseInt(result[3], 16)
            } : null;
        }

        //Door Lock --------------------------------------------------------------------

        $("#lock_button").button().click(lockDoor);
        $("#unlock_button").button().click(unlockDoor);
        $("#set_timer").button().click(setTimer);

        var lockIndicator = $("#lock_indicator");
        var lockIndicatorImg = $("#lock_indicator_img");
        var lockLabel = lockIndicator.find("label");

        function lockDoor(event)
        {
            POST_ACTION(LOCK, 'true');
            lockIndicatorImg.attr("src", "res/locked.png");
        }

        function unlockDoor(event)
        {
            POST_ACTION(LOCK, 'false');
            lockIndicatorImg.attr("src", "res/unlocked.png");
        }

        //Timer Selector ----------------------------------------------------------------

        $( "#radio" ).buttonset();
        $("#switch4").button().click(radioSelect);
        $("#led2").button().click(radioSelect);

        var lock_selector = $("#lock_selector");
        var led_selector = $("#led_selector");
        lock_selector.buttonset();

        var timer = $('#timer').wickedpicker(
            {
                title: "Timer"
            }
        );

        function radioSelect(event)
        {
            lock_selector.hide();
            led_selector.hide();

            switch (this.id)
            {
                case LOCK:
                    lock_selector.show();
                    break;
                case LED2:
                    led_selector.show();
                    break;
                default:
            }
        }

        function setTimer(event) 
        {
            var time = timer.wickedpicker('time').replace(/\s/g, '');

            //Convert to 24 hour
            if (time.indexOf('PM') > -1) 
            {
                var hour = parseInt(time.substring(0, time.indexOf(':')));

                hour = (hour == 12 ? 0 : hour + 12);
                time = hour + ":" + time.substring(time.indexOf(':') + 1, time.indexOf(':') + 3);
            }
            else 
            {
                time = time.replace("AM", "");
            }

            var command = $("#radio :checked").attr('id');
            var data;

            switch (command)
            {
                case LED2:
                    var colorObject = flat_selector.spectrum("get").toRgb();
                    data = colorObject.r + " " + colorObject.b + " " + colorObject.g;
                    break;
                case LOCK:
                    data = ($("#lock_selector :checked").attr('id') == "Lock" ? "true" : "false");
                    break;
                default:
                    return;
            }

            POST_TIMER(time , command, data);
        }
        //Timer table -------------------------------------------------------------------------

        var timers_table = $("#timers_table");

        function loadTimers(result, status, xhr)
        {
            var tableData;

            for(var x = 0; x < result.length; x++)
            {
                tableData += "<tr><td>" + result[x].time + "</td><td>" + result[x].command + "</td><td>" + result[x].data +
                    "</td><td><div id='" + result[x].time + " " + result[x].command + "'>close</div></td></tr>";
            }

            timers_table.html(tableData);
            $("#timers_table tr div").click(timerDelete);
        }

        function timerDelete(event)
        {
            var time_command = this.id.split(" ");

            DELETE_TIMER(time_command[0], time_command[1]);
        }
    }());

    function GET_STATES(callback)
    {
        jQuery.ajax(
            {
                async: true,
                type: "GET",
                contentType: "application/json",
                dataType: "json",
                url: "/action",
                success: callback,
                error: function (xhr, status, error)
                {

                }
            });
    }

    function POST_ACTION(commandForm, dataForm)
    {
        jQuery.ajax(
            {
                async: true,
                type: "POST",
                contentType: "application/json",
                dataType: "json",
                url: "/action/add",
                data:JSON.stringify(
                {
                    "device": commandForm,
                    "data": dataForm
                }),
                success: function (result,status,xhr)
                {

                },
                error: function (xhr,status,error)
                {

                }
            });
    }

    function GET_TIMERS(callback)
    {
        jQuery.ajax(
            {
                async: true,
                type: "GET",
                contentType: "application/json",
                dataType: "json",
                url: "/timer",
                success: callback,
                error: function (xhr, status, error)
                {

                }
            });
    }

    function POST_TIMER(time, commandForm, dataForm)
    {
        jQuery.ajax(
            {
                async: true,
                type: "POST",
                contentType: "application/json",
                dataType: "json",
                url: "/timer/add",
                data:JSON.stringify(
                {
                    "time": time,
                    "evaluated": "false",
                    "action":
                    {
                        "device": commandForm,
                        "data": dataForm
                    }
                }),
                success: function (result,status,xhr)
                {

                },
                error: function (xhr,status,error)
                {

                }
            });
    }

    function DELETE_TIMER(time, command)
    {
        jQuery.ajax(
            {
                async: true,
                type: "POST",
                url: "execute_command.php?action=5",
                data: 'time=' + time + '&command=' + command,
                success: function (result,status,xhr)
                {

                },
                error: function (xhr,status,error)
                {

                }
            });
    }
});