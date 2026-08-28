using System;
using System.IO;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using Newtonsoft.Json;

namespace MailReaderTest
{
    public class Handler1 : IHttpHandler
    {
        private string base_dir = "/msg";

        public void ProcessRequest(HttpContext context)
        {
            string sub_dir = context.Request.
                QueryString["test"] != null ? "test/" : "core/";

            string[] files = Directory.GetFiles(
                context.Server.MapPath(this.base_dir) + "/" + sub_dir, "*.msg");

            for (int i = 0; i < files.Length; i++) {
                files[i] = this.base_dir + "/" + sub_dir + Path.GetFileName(files[i]);
            }

            context.Response.ContentType = "text/json";
            context.Response.Write(JsonConvert.SerializeObject(files));
        }

        public bool IsReusable
        {
            get
            {
                return false;
            }
        }
    }
}