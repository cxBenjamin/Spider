package org.csip.bigData.crawler.util;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.userauth.keyprovider.PKCS8KeyFile;
import net.schmizz.sshj.userauth.method.AuthPublickey;

import java.io.File;
import java.io.IOException;

/**
 * Created by csip on 8/12/16.
 */
public class EC2ConnectUtil {

        static final SSHClient ssh =new SSHClient();
        static  Session session =null;



    public void Ec2ConnectStart()
    {
        try {
            ssh.addHostKeyVerifier("");
            ssh.connect("54.223.50.219");
            PKCS8KeyFile keyFile = new PKCS8KeyFile();
            keyFile.init(new File("csip-robot.pem"));
            ssh.auth("ec2-user",new AuthPublickey(keyFile));
            session=ssh.startSession();


        } catch (IOException e) {
            e.printStackTrace();
        }


    }
    public void Ec2ConnectStop()
    {
        try {
            session.close();
            ssh.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
