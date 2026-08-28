let body: BodyView;
let output: HtmlConsole;

class HtmlConsole
{
    private elem: HTMLPreElement;    

    constructor()
    {
        this.elem = document.getElementById("console") as HTMLPreElement;
    }

    public clear()
    {
        this.elem.innerHTML = "";
    }

    public log(msg: any)
    {
        if (typeof msg == 'object')
        {
            this.elem.innerHTML += (JSON && JSON.stringify ?
                JSON.stringify(msg, undefined, 4) : String(msg)) + '<br />';

            return;
        }

        this.elem.innerHTML += msg + '<br>';
    }
}

class BodyView
{
    private document: HTMLDocument;
    private elem: HTMLIFrameElement;

    constructor()
    {
        this.elem = <HTMLIFrameElement>(document.getElementById("body"));
        this.document = this.elem.contentWindow.document;
    }

    public hide()
    {
        this.elem.style.display = "none";
    }

    private base()
    {
        let base = document.createElement("base");

        base.target = "_blank";

        this.document.head.appendChild(base);
    }

    private style()
    {
        let css = document.createElement("link");

        css.href = "/source/static/iframe.css";
        css.rel = "stylesheet";
        css.type = "text/css";

        this.document.head.appendChild(css);
    }

    public setContent(html?: string)
    {
        if (html != undefined)
        {
            this.document.open();
            this.document.write(html);
            this.document.close();

            this.style();
            this.base();
        }
    }
}

type AJAXCallbackT = (err: any, data?: any) => void;

function ajaxRequest(url: string, type: string, callback: AJAXCallbackT)
{
    let xhr = new XMLHttpRequest();

    xhr.open('GET', url, true);
    xhr.responseType = <any>type;

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

import MR = MailReader.MSG;

function loadMail(path: string, callback?: Function)
{
    ajaxRequest(path, "arraybuffer",
        (err: any, data: any) =>
    {
        output.clear();
        body.setContent("");

        output.log("URL of FILE: " + path);
        output.log("");
        
        if (err == undefined)
        {
            try
            {
                // Parse msg file 

                let msg = new MailReader.MSG.Mail(data);

                // Output some info

                output.log("PidTagSubject: " + msg.subject);
                output.log("PidTagDisplayTo: " + msg.displayTo);
                output.log("PidTagSenderName: " + msg.senderName);
                output.log("PidTagSenderAddress: " + msg.senderAddress);
                output.log("");
                output.log("PidTagDeliveryTime: " + msg.deliveryTime);

                if (msg.displayCc != undefined) {
                    output.log("PidTagDisplayCc: " + msg.displayCc);
                }

                // Iterate recipients

                let recips = msg.recipients;

                for (let i = 0; i < recips.length; i++)
                {
                    output.log("");
                    output.log("Recipient #" + i + ":");
                    output.log("Display name: " + recips[i].name);
                    output.log("Email address: " + recips[i].email);
                    output.log("Type: " + recips[i].type);
                }

                // Iterate attachments

                let attachs = msg.attachments;

                for (let i = 0; i < attachs.length; i++)
                {
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
            catch (e)
            {
                err = e;
            }
        }

        output.log(err.stack != undefined ? err.stack : err.description);
        output.log("\n");
    });
}

document.addEventListener("DOMContentLoaded", () =>
{
    body = new BodyView();
    output = new HtmlConsole();

    let ttimeout = 0;
    let trunning = false;
    let get_mail: (add: boolean) => string;

    document.getElementById("prev-btn")!.onclick = () => loadMail(get_mail(false));
    document.getElementById("next-btn")!.onclick = () => loadMail(get_mail(true));
    document.getElementById("test-btn")!.onclick = () => {
        trunning = !trunning;
        if (trunning) {
            let tload = () => {
                clearTimeout(ttimeout);
                loadMail(get_mail(true), () => {
                    if (trunning) {
                        ttimeout = setTimeout(function () { tload() }, 2000);
                    }
                });
            }
        }
    };

    let url = "/source/asp/msglist.ashx";// + "?test=true";

    ajaxRequest(url, "text",
        (err: any, data: any) =>
    {
        if (err == undefined)
        {
            data = JSON.parse(data);

            let mail: number = 0;

            get_mail = (add: boolean): string => {
                mail = (mail - 1) < 0 ? data.length : mail;
                return data[Math.abs((add ? ++mail : --mail)) % data.length];
            };

            loadMail(data[mail]);

            return;
        }

        output.log(err.stack != undefined ? err.stack : err.description);
        output.log("\n");
    });
});