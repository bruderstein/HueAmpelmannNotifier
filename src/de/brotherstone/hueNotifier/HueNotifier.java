package de.brotherstone.hueNotifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;




import com.intellij.openapi.diagnostic.Log;

import jetbrains.buildServer.Build;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.notification.Notificator;
import jetbrains.buildServer.notification.NotificatorRegistry;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.responsibility.TestNameResponsibilityEntry;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.UserPropertyInfo;
import jetbrains.buildServer.serverSide.mute.MuteInfo;
import jetbrains.buildServer.serverSide.problems.BuildProblemInfo;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.users.NotificatorPropertyKey;
import jetbrains.buildServer.users.PropertyKey;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.vcs.VcsRoot;

/**
 * This teamcity plugins is configured as a new notificator. The notifications are text
 * messages, which are sended to the text to speech engine of the Nabaztag.
 * For each teamcity user it is possible to configure his Nabaztag settings and notification events. In this
 * way multiple Nabaztags could be accessed for different projects or developers.
 * The configuration could be found in the same place, where mail or ide notifications are configured.
 * <p/>
 * User: Simon Tiffert
 * Copyright: Viaboxx GmbH 2010
 * <p/>
 * Changes made by Daniel Wellman (dwellman@cyrusinnovation.com):
 * - Renamed the Rabbit ID display field to clearly indicate it's the serial number
 * - Fixed a spelling error in successful
 * - You can now specify a voice to use (e.g. UK-Penelope).  For a list of
 * voices, send your rabbit this command:
 * http://api.nabaztag.com/vl/FR/api.jsp?sn=MYSERIALNUMBER&token=MYTOKEN&action=9
 * <p/>
 * Changes made by Robert Moran:
 * - Exposed all build messages to the user for customisation
 * - Added #PROJECT#, #USER# and #COMMENT# placeholders which can be used
 * in the messages and are replaced with the project name, user or username of
 * the last person to make a change and their commit comments
 * <p/>
 * - if no voice is specified a random voice is picked
 * - now returns multiple users and comments for build
 * - replaces '#' in message as this truncates what nabaztag says
 * - added message length limit and text to add to end of truncated message
 */
public class HueNotifier implements Notificator {

    private static final String TYPE = "HueNotifier";
    private static final String TYPE_NAME = "Hue Notifier";
    private static final String SERVER_ADDRESS = "hueBuildServerAddress";
   
    private static final PropertyKey SERVER_ADDRESS_KEY = new NotificatorPropertyKey(TYPE, SERVER_ADDRESS);

    public HueNotifier(NotificatorRegistry notificatorRegistry) throws IOException {
        ArrayList<UserPropertyInfo> userProps = new ArrayList<UserPropertyInfo>();
        userProps.add(new UserPropertyInfo(SERVER_ADDRESS, "Hue State Server addresss"));
        
        // userProps.add(new UserPropertyInfo(NABAZTAG_MAX_MESSAGE_LENGTH, "Max Message Length"));
        // userProps.add(new UserPropertyInfo(NABAZTAG_ELLIPSES, "Ellipses"));
        notificatorRegistry.register(this, userProps);
    }

    
    public String getNotificatorType() {
        return TYPE;
    }

    public String getDisplayName() {
        return TYPE_NAME;
    }

  


       
	@Override
	public void notifyBuildFailed(SRunningBuild runningBuild, Set<SUser> users) {
		// TODO Auto-generated method stub
		List<BuildProblemData> problems = runningBuild.getFailureReasons();
		boolean isTests = false;
		for(BuildProblemData problem : problems) {
			if (BuildProblemData.TC_FAILED_TESTS_TYPE == problem.getType()) {
				isTests = true;
				break;
			}
		}
		
		doNotification(users, runningBuild, isTests ? "failtests" : "fail");
	}

	@Override
	public void notifyBuildFailedToStart(SRunningBuild runningBuild, Set<SUser> users) {
		// TODO Auto-generated method stub
		doNotification(users, runningBuild, "fail");
	}

	@Override
	public void notifyBuildFailing(SRunningBuild runningBuild, Set<SUser> users) {
		// TODO Auto-generated method stub
		doNotification(users, runningBuild, "fail");
	}

	@Override
	public void notifyBuildProbablyHanging(SRunningBuild arg0, Set<SUser> arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyBuildProblemResponsibleAssigned(
			Collection<BuildProblemInfo> arg0, ResponsibilityEntry arg1,
			SProject arg2, Set<SUser> arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyBuildProblemResponsibleChanged(
			Collection<BuildProblemInfo> arg0, ResponsibilityEntry arg1,
			SProject arg2, Set<SUser> arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyBuildProblemsMuted(Collection<BuildProblemInfo> arg0,
			MuteInfo arg1, Set<SUser> arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyBuildProblemsUnmuted(Collection<BuildProblemInfo> arg0,
			MuteInfo arg1, SUser arg2, Set<SUser> arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyBuildStarted(SRunningBuild arg0, Set<SUser> arg1) {
		
	}

	private void doNotification(SUser notifyUser, String buildName, String result) {
		String serverAddress = notifyUser.getPropertyValue(SERVER_ADDRESS_KEY);
		
		HttpPost post = new HttpPost(serverAddress + "/stateNotify/buildStatus/" + buildName + "/" + result);
		try {
			CloseableHttpClient httpclient = HttpClients.createDefault();
			
			CloseableHttpResponse response1 = httpclient.execute(post);
			StatusLine statusLine = response1.getStatusLine();
			Log.print("" + statusLine.getStatusCode());
			
			response1.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void doNotification(Set<SUser> users, SRunningBuild runningBuild, String result)
	{
		for(SUser notifyUser : users) {
			doNotification(notifyUser, runningBuild.getBuildTypeId(), result);
		}
	}
	
	@Override
	public void notifyBuildSuccessful(SRunningBuild runningBuild, Set<SUser> users) {
		doNotification(users, runningBuild, "success");
	}

	@Override
	public void notifyLabelingFailed(Build arg0, VcsRoot arg1, Throwable arg2,
			Set<SUser> arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyResponsibleAssigned(SBuildType arg0, Set<SUser> arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyResponsibleAssigned(TestNameResponsibilityEntry arg0,
			TestNameResponsibilityEntry arg1, SProject arg2, Set<SUser> arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyResponsibleAssigned(Collection<TestName> arg0,
			ResponsibilityEntry arg1, SProject arg2, Set<SUser> arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyResponsibleChanged(SBuildType arg0, Set<SUser> arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyResponsibleChanged(TestNameResponsibilityEntry arg0,
			TestNameResponsibilityEntry arg1, SProject arg2, Set<SUser> arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyResponsibleChanged(Collection<TestName> arg0,
			ResponsibilityEntry arg1, SProject arg2, Set<SUser> arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyTestsMuted(Collection<STest> arg0, MuteInfo arg1,
			Set<SUser> arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyTestsUnmuted(Collection<STest> arg0, MuteInfo arg1,
			SUser arg2, Set<SUser> arg3) {
		// TODO Auto-generated method stub
		
	}
}