/**
 * Created by Christian Everett on 8/4/2016.
 */
function GET_STATE(device, callback)
{
    jQuery.ajax(
        {
            async: true,
            type: "GET",
            contentType: "application/json",
            dataType: "json",
            url: "/action/" + device,
            headers: 
            {
            	//'Connection': 'keep-alive',
            	'Cache-Control': 'no-cache, no-store, must-revalidate',
            	'Pragma': 'no-cache',
            	'Expires': '0'
            },
            success: callback,
            error: function (xhr, status, error)
            {

            }
        });
}

function GET_STATES(callback)
{
    jQuery.ajax(
        {
            async: true,
            type: "GET",
            contentType: "application/json",
            dataType: "json",
            url: "/action",
            headers: 
            {
            	//'Connection': 'keep-alive',
            	'Cache-Control': 'no-cache, no-store, must-revalidate',
            	'Pragma': 'no-cache',
            	'Expires': '0'
            },
            success: callback,
            error: function (xhr, status, error)
            {

            }
        });
}

function POST_ACTION(device, data, callback)
{
    jQuery.ajax(
        {
            async: true,
            type: "POST",
            contentType: "application/json",
            url: "/action/add",
            headers: 
            {
            	//'Connection': 'keep-alive',
            	'Cache-Control': 'no-cache, no-store, must-revalidate',
            	'Pragma': 'no-cache',
            	'Expires': '0'
            },
            data:JSON.stringify(
                {
                    "device": device,
                    "data": data
                }),
            success: callback,
            error: function (xhr,status,error)
            {
                alert("The Server is currently offline. It should be back in a couple minutes, if not email the support team at: Christian.everett1@gmail.com")
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
            headers: 
            {
            	//'Connection': 'keep-alive',
            	'Cache-Control': 'no-cache, no-store, must-revalidate',
            	'Pragma': 'no-cache',
            	'Expires': '0'
            },
            success: callback,
            error: function (xhr, status, error)
            {

            }
        });
}

function POST_TIMER(time, device, data, callback)
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
                    "action":
                    {
                        "device": device,
                        "data": data
                    }
                }),
            success: callback,
            error: function (xhr,status,error)
            {

            }
        });
}

function CHANGE_TIMER(id, time, device, data, callback)
{
    jQuery.ajax(
        {
            async: true,
            type: "POST",
            contentType: "application/json",
            dataType: "json",
            url: "/timer/" + id,
            data:JSON.stringify(
                {
                    "time": time,
                    "action":
                    {
                        "device": device,
                        "data": data
                    }
                }),
            success: callback,
            error: function (xhr,status,error)
            {

            }
        });
}

function DELETE_TIMER(id, callback)
{
    jQuery.ajax(
        {
            async: true,
            type: "DELETE",
            url: "/timer/" + id,
            success: callback,
            error: function (xhr,status,error)
            {

            }
        });
}

function DELETE_ALL(callback)
{
    jQuery.ajax(
        {
            async: true,
            type: "DELETE",
            url: "/timer",
            success: callback,
            error: function (xhr,status,error)
            {

            }
        });
}