var body;
var output;
var HtmlConsole = (function () {
    function HtmlConsole() {
        this.elem = document.getElementById("console");
    }
    HtmlConsole.prototype.clear = function () {
        this.elem.innerHTML = "";
    };
    HtmlConsole.prototype.log = function (msg) {
        if (typeof msg == 'object') {
            this.elem.innerHTML += (JSON && JSON.stringify ?
                JSON.stringify(msg, undefined, 4) : String(msg)) + '<br />';
            return;
        }
        this.elem.innerHTML += msg + '<br>';
    };
    return HtmlConsole;
}());
var BodyView = (function () {
    function BodyView() {
        this.elem = (document.getElementById("body"));
        this.document = this.elem.contentWindow.document;
    }
    BodyView.prototype.hide = function () {
        this.elem.style.display = "none";
    };
    BodyView.prototype.base = function () {
        var base = document.createElement("base");
        base.target = "_blank";
        this.document.head.appendChild(base);
    };
    BodyView.prototype.style = function () {
        var css = document.createElement("link");
        css.href = "/source/static/iframe.css";
        css.rel = "stylesheet";
        css.type = "text/css";
        this.document.head.appendChild(css);
    };
    BodyView.prototype.setContent = function (html) {
        if (html != undefined) {
            this.document.open();
            this.document.write(html);
            this.document.close();
            this.style();
            this.base();
        }
    };
    return BodyView;
}());
function ajaxRequest(url, type, callback) {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', url, true);
    xhr.responseType = type;
    xhr.onload = function () {
        if (xhr.status !== 0 && xhr.status !== 200) {
            callback(new Error('HTTP Error #' + xhr.status + ': ' + xhr.statusText));
            return;
        }
        callback(null, xhr.response);
    };
    xhr.onerror = function () {
        callback(new Error('Network error.'));
    };
    xhr.send(null);
}
var MR = MailReader.MSG;
function loadMail(path, callback) {
    ajaxRequest(path, "arraybuffer", function (err, data) {
        output.clear();
        body.setContent("");
        output.log("URL of FILE: " + path);
        output.log("");
        if (err == undefined) {
            try {
                var msg = new MailReader.MSG.Mail(data);
                output.log("PidTagSubject: " + msg.subject);
                output.log("PidTagDisplayTo: " + msg.displayTo);
                output.log("PidTagSenderName: " + msg.senderName);
                output.log("PidTagSenderAddress: " + msg.senderAddress);
                output.log("");
                output.log("PidTagDeliveryTime: " + msg.deliveryTime);
                if (msg.displayCc != undefined) {
                    output.log("PidTagDisplayCc: " + msg.displayCc);
                }
                var recips = msg.recipients;
                for (var i = 0; i < recips.length; i++) {
                    output.log("");
                    output.log("Recipient #" + i + ":");
                    output.log("Display name: " + recips[i].name);
                    output.log("Email address: " + recips[i].email);
                    output.log("Type: " + recips[i].type);
                }
                var attachs = msg.attachments;
                for (var i = 0; i < attachs.length; i++) {
                    output.log("");
                    output.log("Attachment #" + i + ":");
                    output.log("Display name: " + attachs[i].displayName);
                    output.log("Method: " + attachs[i].attachMethod);
                    output.log("Object type: " + attachs[i].objectType);
                }
                body.setContent(msg.body);
                output.log("");
                output.log("Body method used: " + MR.BodyMethod[msg.bodyMethod]);
                if (callback != undefined) {
                    callback();
                }
                return;
            }
            catch (e) {
                err = e;
            }
        }
        output.log(err.stack != undefined ? err.stack : err.description);
        output.log("\n");
    });
}
document.addEventListener("DOMContentLoaded", function () {
    body = new BodyView();
    output = new HtmlConsole();
    var ttimeout = 0;
    var trunning = false;
    var get_mail;
    document.getElementById("prev-btn").onclick = function () { return loadMail(get_mail(false)); };
    document.getElementById("next-btn").onclick = function () { return loadMail(get_mail(true)); };
    document.getElementById("test-btn").onclick = function () {
        trunning = !trunning;
        if (trunning) {
            var tload_1 = function () {
                clearTimeout(ttimeout);
                loadMail(get_mail(true), function () {
                    if (trunning) {
                        ttimeout = setTimeout(function () { tload_1(); }, 2000);
                    }
                });
            };
        }
    };
    var url = "/source/asp/msglist.ashx";
    ajaxRequest(url, "text", function (err, data) {
        if (err == undefined) {
            data = JSON.parse(data);
            var mail_1 = 0;
            get_mail = function (add) {
                mail_1 = (mail_1 - 1) < 0 ? data.length : mail_1;
                return data[Math.abs((add ? ++mail_1 : --mail_1)) % data.length];
            };
            loadMail(data[mail_1]);
            return;
        }
        output.log(err.stack != undefined ? err.stack : err.description);
        output.log("\n");
    });
});
//# sourceMappingURL=test.js.map