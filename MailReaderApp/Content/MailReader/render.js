"use strict";

(function ()
{
    var _hostUrl = "";
    var _sitePath = "";

    function addClass(selector, _class)
    {
        document.querySelector(selector).classList.add(_class);
    }

    function removeClass(selector, _class)
    {
        document.querySelector(selector).classList.remove(_class);
    }

    function setText(elem, text)
    {
        var txt = document.createTextNode(text);
        var elem = (typeof elem == "object") ? 
            elem : document.querySelector(elem);

        elem.textContent = txt.textContent;
    };

    function ajaxRequest(url, type, success, fail)
    {
        var xhr = new XMLHttpRequest();

        xhr.open("GET", url, true);
        xhr.responseType = type;

        xhr.onload = success;
        xhr.onerror = success;

        xhr.send(null);
    };

    function showError(error)
    {
        document.querySelector("#error-info").innerHTML = 
            error.stack.replace(/(?:\r\n|\r|\n)/g, "<br />")

        setText("#error-time", new Date().toLocaleString(navigator.language, {
            weekday: 'long', year: 'numeric', month: 'long', day: 'numeric',
            hour: '2-digit', minute: '2-digit', second: '2-digit', timeZoneName: 'short'
        }));

        removeClass("#content", "visible");
        removeClass("#loading", "visible");
        addClass("#error", "visible");
    }

    function writeFrame(data)
    {
        var frame = document.querySelector("#frame").contentDocument;

        frame.write(data);
        frame.close();

        var base = frame.createElement("base");
        var style = frame.createElement("style");

        base.target = "_blank"; // open links in new tab

        style.innerHTML = '_:-ms-lang(x), img { border: none; }' + // fix IE img link border
            'body { font-family: "Segoe UI", "Arial", "sans-serif"; }' // set font

        frame.head.appendChild(base);
        frame.head.appendChild(style);

        return frame;
    }

    function getQueryStringParameter(param)
    {
        var strParams = "";
        var params = document.URL.split("?")[1].split("&");

        for (var i = 0; i < params.length; i = i + 1)
        {
            var singleParam = params[i].split("=");

            if (singleParam[0] == param) {
                return singleParam[1];
            }
        }
    };

    function mapToIcon(num, attach, callback)
    {
        var restEndpoint = _sitePath + "/_api/web/maptoicon(filename='" + attach.displayName + "',progid='',size=0)";

        ajaxRequest(restEndpoint, "text", function () {
            if (this.status == 200) {
                var doc = (new DOMParser()).parseFromString(this.response, "text/xml");
                callback(num, attach, doc.querySelector("MapToIcon").textContent);
                return;
            }
            alert("HTTP Error #" + this.status + ": " + this.statusText);
        }, function () {
            console.log(err);
        });

        return window.location.protocol + "//" + window.location.host + "/" + restEndpoint;
    }

    function getFileByUniqueId(hostUrl, uniqueId, callback) 
    {
        var restEndpoint = _sitePath + "/_api/SP.AppContextSite(@h)/web/" +
            "GetFileById('" + uniqueId + "')/$value?@h='" + hostUrl + "'";

        ajaxRequest(restEndpoint, "arraybuffer", function () {
            if (this.status == 200) {
                callback(new Uint8Array(this.response));
                return;
            }
            alert("HTTP Error #" + this.status + ": " + this.statusText);
        }, function () {
            console.log(err);
        });

        return window.location.protocol + "//" + window.location.host + "/" + restEndpoint;
    };

    function getListItemUnqiueId(hostUrl, listId, itemId, callback)
    {
        var restEndpoint = _sitePath + "/_api/SP.AppContextSite(@h)/web/" +
            "lists('" + listId + "')/items?@h='" + hostUrl + "'&$Select=UniqueId&$filter=ID%20eq%20" + itemId;

        ajaxRequest(restEndpoint, "text", function () {
            if (this.status == 200) {
                debugger;
                var doc = (new DOMParser()).parseFromString(this.response, "text/xml");
                callback(doc.querySelector("UniqueId").textContent);
                return;
            }
            alert("HTTP Error #" + this.status + ": " + this.statusText);
        }, function () {
            console.log(err);
        });
    };

    function renderMail(data) 
    {
        try 
        {
            var msg = new MailReader.MSG.Mail(data.buffer);

            // Set header

            setText("#time", msg.deliveryTime);
            setText("#sender", msg.senderName + " (" + msg.senderAddress + ") ");
            setText("#subject", msg.subject);

            // Parse recipients

            for (var i = 0; i < msg.recipients.length; i++)
            {
                var recip = msg.recipients[i];

                var tr = document.querySelector("tr.recipinfo:nth-of-type(" + recip.type + ")");
                var td = document.querySelectorAll("tr.recipinfo:nth-of-type(" + recip.type + ") > td")[1]

                setText(td, td.textContent + recip.name +
                    (recip.nameIsEmail ? "; " : " <" + recip.email + ">; "));

                tr.classList.add("visible");
            }

            // Parse attachments

            var num_attachs = 0;

            for (var i = 0; i < msg.attachments.length; i++)
            {
                var attach = msg.attachments[i];

                if (attach.hidden == false &&
                    attach.objectType == MailReader.MSG.ObjectType.Attachment &&
                    attach.attachMethod == MailReader.MSG.AttachMethod.ByValue)
                {
                    num_attachs++;

                    mapToIcon(i, attach, function (num, attach, icon) {
                        document.querySelector("ul").insertAdjacentHTML("beforeend", '<li><img src="' + _hostUrl +
                            "/_layouts/15/images/" + icon + '" />' + attach.displayName + '</li>');
                    });
                }
            }

            if (num_attachs > 0) {
                document.querySelector("#attachlist").classList.add("visible");
            }

            writeFrame(msg.body);
        }
        catch (ex)
        {
            showError(ex);
            return;
        }

        removeClass("#loading", "visible");
        addClass("#content", "visible");
    };

    _hostUrl = decodeURIComponent(getQueryStringParameter("SPHostUrl"));
    _sitePath = _hostUrl.substring(_hostUrl.indexOf("/", 8));

    if (_sitePath == _hostUrl)
    {
        _sitePath = "";
    }

    var uniqueId = decodeURIComponent(getQueryStringParameter("UniqueId"));
    if (uniqueId == "undefined")
    {
        var listId = decodeURIComponent(getQueryStringParameter("SPListId"));
        var itemId = decodeURIComponent(getQueryStringParameter("SPListItemId"));

        getListItemUnqiueId(_hostUrl, listId, itemId, function (uniqueId) {
            getFileByUniqueId(_hostUrl, uniqueId, renderMail);
        });
    }
    else {
        getFileByUniqueId(_hostUrl, uniqueId, renderMail);
    }
})();