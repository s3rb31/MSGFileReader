"use strict";

(function ()
{
    var _appurl = "";
    var _dialog = null;
    var _productNum = 0;
    
    function MRDialog(uniqueId)
    {
        if (_appurl != "")
        {
            // Close dialog if open, and then create it

            if (_dialog != null) {
                _dialog.close();
            }

            // Remove the modal overlay

            var style = document.createElement("style");

            style.id = "mr-nomodal";
            style.innerHTML = '.ms-dlgOverlay { display: none !important; }';

            document.head.appendChild(style);

            // Show the dialog

            var dialogWidth = (window.innerWidth || document.body.clientWidth) / 2;

            _dialog = SP.UI.ModalDialog.showModalDialog({
                url: _appurl + "/Content/MailReader/Render.html" +
                    "?SPHostUrl=" + encodeURIComponent(_spPageContextInfo.siteAbsoluteUrl) +
                    "&UniqueId=" + encodeURIComponent(uniqueId),
                title: "GFA SysCom - Mailreader",
                allowMaximize: false,
                showClose: true,
                height: 750,
                width: dialogWidth < 800 ? 800 : dialogWidth,
                dialogReturnValueCallback: Function.createDelegate(this, function() {
                    var style = document.querySelector("#mr-nomodal");
                    style.parentNode.removeChild(style);
                })
            });

            console.log(_dialog.$2_0.url);

            // Position the dialog nicely (M$ can't do it right ... sad story)

            var dialog = document.querySelector(".ms-dlgContent");
            dialog.style.top = (window.innerHeight - dialog.offsetHeight) / 2 + "px"

            return;
        }

        console.error("[MailReader] App URL not found. Check if the app is properly installed!")
    };
    
    function newRenderType(ctx, field, listItem, listSchema)
    {			
        var type = listItem.File_x0020_Type;
        var html = ComputedFieldWorker.DocIcon(ctx, field, listItem, listSchema);

        if (type != undefined && type.toLowerCase() == "msg") 
        {
            html = html.substr(0, html.length - 2) + 
                ' onclick="MRDialog(\'' + listItem.UniqueId + '\');"/>';
        }

        return html;
    };

    function init()
    {
        window.MRDialog = MRDialog;

        var iconContext = {};
        iconContext.Templates = {};
        iconContext.Templates.Fields = {
            "DocIcon": { "View": newRenderType },
            "MasterPageIcon": { "View": newRenderType }
        };

        SPClientTemplates.TemplateManager.RegisterTemplateOverrides(iconContext);

        ExecuteOrDelayUntilScriptLoaded(function () {
            if (_appurl == false) {
                var context = SP.ClientContext.get_current()
                var appInstList = SP.AppCatalog.getAppInstances(context, context.get_web());

                context.load(appInstList);
                context.executeQueryAsync(function () {
                    _productNum = context.get_serverVersion();
                    var list = appInstList.getEnumerator();
                    while (list.moveNext()) {
                        var current = list.get_current();
                        if (current.get_title() == "MSG MailReader") {
                            _appurl = current.get_appWebFullUrl();
                            return;
                        }
                    }
                });
            }
        }, "sp.js");
    };
    
    if (typeof g_MinimalDownload != "undefined" &&
        typeof asyncDeltaManager != "undefined" && g_MinimalDownload)
    {
        RegisterBeginEndFunctions("clienttemplates.js", "mrinit", null, init, null);
    }

    ExecuteOrDelayUntilScriptLoaded(init, "clienttemplates.js");
})();
