using System;
using System.Diagnostics;

internal static class Uninstall
{
    [STAThread]
    private static int Main()
    {
        try
        {
            Process.Start(new ProcessStartInfo("msiexec.exe", "/x @PRODUCT_CODE@") { UseShellExecute = true });
            return 0;
        }
        catch { return 1; }
    }
}
