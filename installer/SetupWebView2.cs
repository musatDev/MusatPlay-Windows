using System;
using System.Diagnostics;
using System.Drawing;
using System.Globalization;
using System.IO;
using System.Reflection;
using System.Windows.Forms;

[assembly: AssemblyTitle("MusatPlay Setup")]
[assembly: AssemblyProduct("MusatPlay")]
[assembly: AssemblyVersion("@VERSION@.0")]

internal static class SetupWebView2
{
    internal static bool RuntimeAvailable()
    {
        foreach (string root in new[] { Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86),
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData) })
        {
            string path = Path.Combine(root, "Microsoft", "EdgeWebView", "Application");
            try { if (Directory.Exists(path)) foreach (string version in Directory.GetDirectories(path))
                if (File.Exists(Path.Combine(version, "msedgewebview2.exe"))) return true; }
            catch (IOException) { } catch (UnauthorizedAccessException) { }
        }
        return false;
    }
    [STAThread]
    private static int Main(string[] args)
    {
        if (args.Length == 1 && args[0] == "--verify-webview2") return RuntimeAvailable() ? 0 : 2;
        Application.EnableVisualStyles();
        Application.SetCompatibleTextRenderingDefault(false);
        while (!RuntimeAvailable())
        {
            using (var missing = new Form { Text="MusatPlay - WebView2", ClientSize=new Size(540,220),
                FormBorderStyle=FormBorderStyle.FixedDialog, MaximizeBox=false, MinimizeBox=false,
                StartPosition=FormStartPosition.CenterScreen, Font=new Font("Segoe UI",10), AutoScaleMode=AutoScaleMode.Dpi })
            {
                var text=new Label { Left=20,Top=20,Width=500,Height=115, Text="MusatPlay needs Microsoft Edge WebView2. Install it from Microsoft, then click Check again." };
                var download=new Button {Left=20,Top=160,Width=210,Text="Download WebView2"};
                download.Click+=delegate { try { Process.Start(new ProcessStartInfo("https://developer.microsoft.com/microsoft-edge/webview2/"){UseShellExecute=true}); }
                    catch(Exception error){MessageBox.Show(error.Message);} };
                var retry=new Button {Left=280,Top=160,Width=240,Text="Check again",DialogResult=DialogResult.OK};
                missing.Controls.AddRange(new Control[]{text,download,retry});
                if(missing.ShowDialog()!=DialogResult.OK) return 1602;
            }
        }
        string directory=Path.Combine(Path.GetTempPath(),"MusatPlay-Setup-"+Guid.NewGuid().ToString("N"));
        string msi=Path.Combine(directory,"MusatPlay.msi");
        try
        {
            Directory.CreateDirectory(directory);
            Extract("MusatPlay.msi",msi);

            string command="/i \""+msi+"\"";
            using(Process process=Process.Start(new ProcessStartInfo(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.System),"msiexec.exe"),command){UseShellExecute=false}))
            { process.WaitForExit(); return process.ExitCode; }
        }
        catch(Exception error){MessageBox.Show(error.Message,"MusatPlay Setup",MessageBoxButtons.OK,MessageBoxIcon.Error);return 1;}
        finally{try{File.Delete(msi);Directory.Delete(directory);}catch(IOException){}catch(UnauthorizedAccessException){}}
    }
    private static void Extract(string name,string path)
    {
        using(Stream source=Assembly.GetExecutingAssembly().GetManifestResourceStream(name))
        using(var output=File.Create(path)) source.CopyTo(output);
    }
}
