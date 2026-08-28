"use strict";

(function ()
{
    var context = null, web = null, actions = null;

    function outputText(text) 
    {
        document.querySelector("#message").textContent = text;
    }

    function outputError(sender, args)
    {
        var msg = args.get_message();
        var err = args.get_errorCode();

        document.querySelector("#message").textContent = msg + " [" + err + "]";
    };

    function getQueryStringParameter(param)
    {
        var strParams = "";
        var params = document.URL.split("?")[1].split("&");

        for (var i = 0; i < params.length; i = i + 1)
        {
            var singleParam = params[i].split("=");

            if (singleParam[0] == param) 
                return singleParam[1];
        }
    };

    function init(callback)
    {
        context = SP.ClientContext.get_current();

        var hostContext = new SP.AppContextSite(context,
            decodeURIComponent(getQueryStringParameter("SPHostUrl")));

        web = hostContext.get_web();
        actions = web.get_userCustomActions();

        context.load(web);
        context.load(actions);
        context.executeQueryAsync(callback, outputError);
    };

    function removeCustomAction(name, callback)
    {
        outputText("Removing custom action from host web...")

        var removeThese = []
        var enumerator = actions.getEnumerator();

        while (enumerator.moveNext())
        {
            var action = enumerator.get_current();

            if (action.get_title() == name &&
                action.get_location() == "ScriptLink" &&
                action.get_description() == "SysCom.SharePointMailReader")
            {
                removeThese.push(action)
            }
        }

        for (var i in removeThese)
        {
            removeThese[i].deleteObject()
            delete removeThese[i]
        }

        if (typeof callback != "undefined") {
            context.executeQueryAsync(callback, outputError);
        }
    };

    function addScriptCustomAction(name, script, callback)
    {
        var xhr = new XMLHttpRequest();
        xhr.open("GET", script + "?ver=" + ((new Date()) * 1), true);

        xhr.onload = function () 
        {
            if (xhr.status !== 0 && xhr.status !== 200) 
            {
                outputText("HTTP Error #" + 
                    xhr.status + ": " + xhr.statusText)

                return;
            }

            // Check if the custom action already exists, if
            // yes then remove it before adding the new one

            removeCustomAction(name);

            outputText("Adding custom action to host web...");

            var newAction = actions.add();
            newAction.set_title(name);
            newAction.set_location("ScriptLink");
            newAction.set_description("SysCom.SharePointMailReader");
            newAction.set_scriptBlock(xhr.response);
            newAction.update();

            context.executeQueryAsync(callback(), outputError);
        };

        xhr.onerror = function () {
            outputText("Network error.")
        };

        xhr.send(null);
    };

    ExecuteOrDelayUntilScriptLoaded(function ()
    {
        outputText("");

        document.querySelector("#btnEmbed").addEventListener("click", function () {
            init(function () {
                addScriptCustomAction("MRSetup", "MailReader/setup.js", function () {
                    outputText("Embed done!");
                });
            });
        });

        document.querySelector("#btnRemove").addEventListener("click", function () {
            init(function () {
                removeCustomAction("MRSetup", function () {
                    outputText("Removal done!")
                });
            });
        });
    }, "sp.js");
})();