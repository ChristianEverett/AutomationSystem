/**
 * Created by Christian Everett on 8/4/2016.
 */

function CREATE_STATE(device, params)
{
 return JSON.stringify(
     {
         "name": device,
         "params": params
     });
}

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
            url: "/action/" + device,
            headers: {
                //'Connection': 'keep-alive',
                'Cache-Control': 'no-cache, no-store, must-revalidate',
                'Pragma': 'no-cache',
                'Expires': '0'
            },
            data: CREATE_STATE(device, data),
            success: callback,
            error: function (xhr, status, error)
            {
                if (xhr.status == 403)
                {
                    alert("This Device is Locked");
                }
                else
                {
                    alert("The Server is currently offline. It should be back in a couple minutes, if not email the support team at: Christian.everett1@gmail.com")
                }
            }
        });
}