/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.services.locking;

import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.locking.handler.LockHandlerFactory;
import eu.europa.ec.leos.test.support.LeosTest;
import eu.europa.ec.leos.test.support.model.ModelHelper;
import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.vo.lock.LockData;
import eu.europa.ec.leos.vo.lock.LockLevel;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:eu/europa/ec/leos/test-lockingServicesContext.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class LockingServiceImplTest extends LeosTest {

    @Mock
    private LockUpdateBroadcaster lockInfoBroadcaster;

    @Autowired
    private LockHandlerFactory lockHandlerFactory;

    @Autowired
    @InjectMocks
    private LockingServiceImpl lockingServiceImpl;

    @Test
    public void test_lockDocument_ReadLevel_successfull() throws Exception {

        User user = ModelHelper.buildUser(33L, "userA", "user A");

        // DO THE ACTUAL CALL
        LockActionInfo lockAcquireInfo = lockingServiceImpl.lockDocument("45", user, "sessionID",LockLevel.READ_LOCK);

        assertThat(lockAcquireInfo.sucesss(), is(true));

        List<LockData> lockData = lockAcquireInfo.getCurrentLocks();

        assertThat(lockData.size(), is(1));
        assertThat(lockData.get(0).getLockId(), is("45"));
        assertThat(lockData.get(0).getUserLoginName(), is("userA"));
        assertThat(lockData.get(0).getUserName(), is("user A"));

        verify(lockInfoBroadcaster).broadcastLockUpdate(lockAcquireInfo);
    }
    @Test
    public void test_lockDocument_DocumentLevel_successfull() throws Exception {

        User user = ModelHelper.buildUser(33L, "userA", "user A");

        // DO THE ACTUAL CALL
        LockActionInfo lockAcquireInfo = lockingServiceImpl.lockDocument("45", user, "sessionID",LockLevel.DOCUMENT_LOCK);

        assertThat(lockAcquireInfo.sucesss(), is(true));

        List<LockData> lockData = lockAcquireInfo.getCurrentLocks();

        assertThat(lockData.size(), is(1));
        assertThat(lockData.get(0).getLockId(), is("45"));
        assertThat(lockData.get(0).getUserLoginName(), is("userA"));
        assertThat(lockData.get(0).getUserName(), is("user A"));

        verify(lockInfoBroadcaster).broadcastLockUpdate(lockAcquireInfo);
    }
    @Test
    public void test_lockDocument_ElementLevel_successfull() throws Exception {

        User user = ModelHelper.buildUser(33L, "userA", "user A");

        // DO THE ACTUAL CALL
        LockActionInfo lockAcquireInfo = lockingServiceImpl.lockDocument("45", user, "sessionID",LockLevel.ELEMENT_LOCK, "art1");

        assertThat(lockAcquireInfo.sucesss(), is(true));

        List<LockData> lockData = lockAcquireInfo.getCurrentLocks();

        assertThat(lockData.size(), is(1));
        assertThat(lockData.get(0).getLockId(), is("45"));
        assertThat(lockData.get(0).getUserLoginName(), is("userA"));
        assertThat(lockData.get(0).getUserName(), is("user A"));

        verify(lockInfoBroadcaster).broadcastLockUpdate(lockAcquireInfo);
    }
    
    @Test
    public void test_lockDocument_Read_whenAlready_Locked_successfull() throws Exception {

        User userA = ModelHelper.buildUser(33L, "userA", "user A");
        User userB = ModelHelper.buildUser(33L, "userB", "user B");

        // DO THE ACTUAL CALL
        LockActionInfo lockAcquireInfoA = lockingServiceImpl.lockDocument("45", userA, "sessionIDA",LockLevel.READ_LOCK);
        LockActionInfo lockAcquireInfoB = lockingServiceImpl.lockDocument("45", userB, "sessionIDB",LockLevel.READ_LOCK);

        assertThat(lockAcquireInfoA.sucesss(), is(true));
        assertThat(lockAcquireInfoB.sucesss(), is(true));
        
        List<LockData> lockInfoA = lockAcquireInfoA.getCurrentLocks();
        List<LockData> lockInfoB = lockAcquireInfoB.getCurrentLocks();

        assertThat(lockInfoA.size(), is(1));
        assertThat(lockInfoA.get(0).getLockId(), is("45"));
        assertThat(lockInfoA.get(0).getUserLoginName(), is("userA"));
        assertThat(lockInfoA.get(0).getUserName(), is("user A"));

        assertThat(lockInfoB.size(), is(2));
        assertThat(lockInfoB.get(1).getLockId(), is("45"));
        assertThat(lockInfoB.get(1).getUserLoginName(), is("userB"));
        assertThat(lockInfoB.get(1).getUserName(), is("user B"));
        
        verify(lockInfoBroadcaster, times(1)).broadcastLockUpdate(lockAcquireInfoA);
        verify(lockInfoBroadcaster, times(1)).broadcastLockUpdate(lockAcquireInfoB);
    }
    
    @Test
    public void test_lockElement_whenAnotherElementAlreadyLocked_successfull() throws Exception {

        User userA = ModelHelper.buildUser(33L, "userA", "user A");
        User userB = ModelHelper.buildUser(33L, "userB", "user B");

        // DO THE ACTUAL CALL
        LockActionInfo lockAcquireInfoA = lockingServiceImpl.lockDocument("45", userA, "sessionIDA",LockLevel.ELEMENT_LOCK, "art1");
        LockActionInfo lockAcquireInfoB = lockingServiceImpl.lockDocument("45", userB, "sessionIDB",LockLevel.ELEMENT_LOCK, "art2");

        assertThat(lockAcquireInfoA.sucesss(), is(true));
        assertThat(lockAcquireInfoB.sucesss(), is(true));
        
        List<LockData> lockInfoA = lockAcquireInfoA.getCurrentLocks();
        List<LockData> lockInfoB = lockAcquireInfoB.getCurrentLocks();

        assertThat(lockInfoA.size(), is(1));
        assertThat(lockInfoA.get(0).getLockId(), is("45"));
        assertThat(lockInfoA.get(0).getUserLoginName(), is("userA"));
        assertThat(lockInfoA.get(0).getUserName(), is("user A"));
        assertThat(lockInfoA.get(0).getElementId(), is("art1"));
        
        assertThat(lockInfoB.size(), is(2));
        assertThat(lockInfoB.get(1).getLockId(), is("45"));
        assertThat(lockInfoB.get(1).getUserLoginName(), is("userB"));
        assertThat(lockInfoB.get(1).getUserName(), is("user B"));
        assertThat(lockInfoB.get(1).getElementId(), is("art2"));
        
        verify(lockInfoBroadcaster, times(1)).broadcastLockUpdate(lockAcquireInfoB);
        verify(lockInfoBroadcaster, times(1)).broadcastLockUpdate(lockAcquireInfoB);
    }
    
    @Test
    public void test_lockSameElement_whenElementAlready_Locked_returnOldLock() throws Exception {
        User userA = ModelHelper.buildUser(33L, "userA", "user A");
        User userB = ModelHelper.buildUser(33L, "userB", "user B");

        // DO THE ACTUAL CALL
        LockActionInfo lockAcquireInfoA = lockingServiceImpl.lockDocument("45", userA, "sessionIDA",LockLevel.ELEMENT_LOCK, "art1");
        LockActionInfo lockAcquireInfoB = lockingServiceImpl.lockDocument("45", userB, "sessionIDB",LockLevel.ELEMENT_LOCK, "art1");

        assertThat(lockAcquireInfoA.sucesss(), is(true));
        assertThat(lockAcquireInfoB.sucesss(), is(false));
        
        List<LockData> lockInfoA = lockAcquireInfoA.getCurrentLocks();
        List<LockData> lockInfoB = lockAcquireInfoB.getCurrentLocks();

        assertThat(lockInfoA.size(), is(1));
        assertThat(lockInfoA.get(0).getLockId(), is("45"));
        assertThat(lockInfoA.get(0).getUserLoginName(), is("userA"));
        assertThat(lockInfoA.get(0).getUserName(), is("user A"));
        assertThat(lockInfoA.get(0).getElementId(), is("art1"));
        
        assertThat(lockInfoB.size(), is(1));
        assertThat(lockInfoB.get(0).getLockId(), is("45"));
        assertThat(lockInfoB.get(0).getUserLoginName(), is("userA"));
        assertThat(lockInfoB.get(0).getUserName(), is("user A"));
        assertThat(lockInfoB.get(0).getElementId(), is("art1"));
        
        verify(lockInfoBroadcaster, times(1)).broadcastLockUpdate(lockAcquireInfoA);
    }
    
    @Test
    public void test_lockDocument_Element_whenElementAlready_Locked_returnFalse() throws Exception {
    	//same user should not be able to lock the same article
        User userA = ModelHelper.buildUser(33L, "userA", "user A");

        // DO THE ACTUAL CALL
        LockActionInfo lockAcquireInfoA = lockingServiceImpl.lockDocument("45", userA, "sessionIDA",LockLevel.ELEMENT_LOCK, "art1");
        LockActionInfo lockAcquireInfoB = lockingServiceImpl.lockDocument("45", userA, "sessionIDB",LockLevel.ELEMENT_LOCK, "art1");

        assertThat(lockAcquireInfoA.sucesss(), is(true));
        assertThat(lockAcquireInfoB.sucesss(), is(false));
        
        List<LockData> lockInfoA = lockAcquireInfoA.getCurrentLocks();
        List<LockData> lockInfoB = lockAcquireInfoB.getCurrentLocks();

        assertThat(lockInfoA.size(), is(1));
        assertThat(lockInfoA.get(0).getLockId(), is("45"));
        assertThat(lockInfoA.get(0).getUserLoginName(), is("userA"));
        assertThat(lockInfoA.get(0).getUserName(), is("user A"));
        assertThat(lockInfoA.get(0).getElementId(), is("art1"));
        
        assertThat(lockInfoB.size(), is(1));
        assertThat(lockInfoB.get(0).getLockId(), is("45"));
        assertThat(lockInfoB.get(0).getUserLoginName(), is("userA"));
        assertThat(lockInfoB.get(0).getUserName(), is("user A"));
        assertThat(lockInfoB.get(0).getElementId(), is("art1"));
        
        verify(lockInfoBroadcaster, times(1)).broadcastLockUpdate(lockAcquireInfoA);
    }
    
    @Test
    public void test_lockDocument_when_alreadyDocumentLocked_should_returnFalseAndOldLock() throws Exception {
        User user = ModelHelper.buildUser(33L, "userA", "user A");
        User userB = ModelHelper.buildUser(34L, "userB", "user B");

        LockActionInfo existingLock = lockingServiceImpl.lockDocument("45", user, "sessionID",LockLevel.DOCUMENT_LOCK);
        
        // DO THE ACTUAL CALL
        LockActionInfo result = lockingServiceImpl.lockDocument("45", userB, "sessionID",LockLevel.DOCUMENT_LOCK);

        assertThat(result.getCurrentLocks().size(), is(1));
        assertThat(result.getCurrentLocks().get(0), is(sameInstance(existingLock.getCurrentLocks().get(0))));
        assertThat(result.sucesss(), is(false));
        verify(lockInfoBroadcaster, times(1)).broadcastLockUpdate(existingLock);
        verifyNoMoreInteractions(lockInfoBroadcaster);
    }

    @Test
    public void test_lockElement_whenDocumentAlreadyLocked_should_returnNotLock() throws Exception {
        User user = ModelHelper.buildUser(33L, "userA", "user A");
        User userB = ModelHelper.buildUser(34L, "userB", "user B");

        LockActionInfo existingLock = lockingServiceImpl.lockDocument("45", user, "sessionID",LockLevel.DOCUMENT_LOCK);
        
        // DO THE ACTUAL CALL
        LockActionInfo result = lockingServiceImpl.lockDocument("45", userB, "sessionID2",LockLevel.ELEMENT_LOCK,"elementId");

        assertThat(result.getCurrentLocks().size(), is(1));
        assertThat(result.getCurrentLocks().get(0), is(sameInstance(existingLock.getCurrentLocks().get(0))));
        assertThat(result.sucesss(), is(false));
        verify(lockInfoBroadcaster, times(0)).broadcastLockUpdate(result);
        verify(lockInfoBroadcaster, times(1)).broadcastLockUpdate(existingLock);

        verifyNoMoreInteractions(lockInfoBroadcaster);
    }    
    
    @Test(expected = NullPointerException.class)
    public void test_lockDocument_when_leosIdNull_should_throwException() throws Exception {
        // DO THE ACTUAL CALL
        lockingServiceImpl.lockDocument(null, ModelHelper.buildUser(33L, "userA", "user A"), "sessionID",LockLevel.READ_LOCK);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_lockDocument_when_invalidCall_throwException() throws Exception {
        // DO THE ACTUAL CALL
        lockingServiceImpl.lockDocument("45", ModelHelper.buildUser(33L, "userA", "user A"), "sessionID",LockLevel.DOCUMENT_LOCK, "testArt");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_lockDocument_when_invalidReadCall_throwException() throws Exception {

        // DO THE ACTUAL CALL
        lockingServiceImpl.lockDocument("45", ModelHelper.buildUser(33L, "userA", "user A"), "sessionID",LockLevel.READ_LOCK, "testArt");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_lockDocument_when_elementNull_should_throwException() throws Exception {

        // DO THE ACTUAL CALL
        lockingServiceImpl.lockDocument("45", ModelHelper.buildUser(33L, "userA", "user A"), "sessionID",LockLevel.ELEMENT_LOCK, null);

    }
    
    @Test(expected = NullPointerException.class)
    public void test_lockDocument_when_userNull_should_throwException() throws Exception {

        // DO THE ACTUAL CALL
        lockingServiceImpl.lockDocument("a", null, "sessionID",LockLevel.ELEMENT_LOCK,"test");

    }

    @Test
    public void test_unlockDocument_when_noDocLock_should_returnFalse() throws Exception {
        User user = ModelHelper.buildUser(33L, "userA", "user A");

        // DO THE ACTUAL CALL
        LockActionInfo result = lockingServiceImpl.unlockDocument("45", user.getLogin(),"SessionID",LockLevel.DOCUMENT_LOCK);

        assertThat(result.getCurrentLocks(), is(emptyCollectionOf(LockData.class)));
        assertThat(result.sucesss(), is(false));
        verifyZeroInteractions(lockInfoBroadcaster);
    }

    @Test
    public void test_unlockDocument_when_noElementLock_should_returnFalse() throws Exception {
        User user = ModelHelper.buildUser(33L, "userA", "user A");

        // DO THE ACTUAL CALL
        LockActionInfo result = lockingServiceImpl.unlockDocument("45", user.getLogin(),"SessionID",LockLevel.ELEMENT_LOCK, "elementId" );

        assertThat(result.getCurrentLocks(), is(emptyCollectionOf(LockData.class)));
        assertThat(result.sucesss(), is(false));
        verifyZeroInteractions(lockInfoBroadcaster);
    }
    
    @Test
    public void test_unlockDocument_when_noReadLock_should_returnFalse() throws Exception {
        User user = ModelHelper.buildUser(33L, "userA", "user A");

        // DO THE ACTUAL CALL
        LockActionInfo result = lockingServiceImpl.unlockDocument("45", user.getLogin(),"SessionID",LockLevel.READ_LOCK);

        assertThat(result.getCurrentLocks(), is(emptyCollectionOf(LockData.class)));
        assertThat(result.sucesss(), is(false));
        verifyZeroInteractions(lockInfoBroadcaster);
    }    
    
    @Test
    public void test_unlockDocument_when_lock_should_returnTrue() throws Exception {
        User user = ModelHelper.buildUser(33L, "userA", "user A");
        
        lockingServiceImpl.lockDocument("45", user, "SessionID",LockLevel.DOCUMENT_LOCK);

        // DO THE ACTUAL CALL
        LockActionInfo result = lockingServiceImpl.unlockDocument("45", user.getLogin(), "SessionID", LockLevel.DOCUMENT_LOCK);

        assertThat(result.sucesss(), is(true));
        // test that the lock is removed from the map
        assertThat(result.getCurrentLocks(), is(emptyCollectionOf(LockData.class)));
        verify(lockInfoBroadcaster, times(1)).broadcastLockUpdate(result);
    }

    @Test
    public void test_unlockElement_when_ElementAlreadylock_should_returnTrue() throws Exception {
        User user = ModelHelper.buildUser(33L, "userA", "user A");
        
        lockingServiceImpl.lockDocument("45", user, "SessionID",LockLevel.ELEMENT_LOCK, "elementId");

        // DO THE ACTUAL CALL
        LockActionInfo result = lockingServiceImpl.unlockDocument("45", user.getLogin(), "SessionID", LockLevel.ELEMENT_LOCK, "elementId");

        assertThat(result.sucesss(), is(true));
        // test that the lock is removed from the map
        assertThat(result.getCurrentLocks(), is(emptyCollectionOf(LockData.class)));
        verify(lockInfoBroadcaster, times(1)).broadcastLockUpdate(result);
    }
    
    @Test
    public void test_unlockDocument_when_lockedByOtherUser_should_returnOtherLock() throws Exception {
        
    	User user = ModelHelper.buildUser(33L, "userA", "user A");
        User userB = ModelHelper.buildUser(34L, "userB", "user B");

        LockActionInfo existingLock = lockingServiceImpl.lockDocument("45", user, "sessionID_A",LockLevel.DOCUMENT_LOCK);

        // DO THE ACTUAL CALL
        LockActionInfo result = lockingServiceImpl.unlockDocument("45", userB.getLogin(),"sessionID_B", LockLevel.DOCUMENT_LOCK );

        assertThat(result.getCurrentLocks().size(), is(1));
        assertThat(result.sucesss(), is(false));
        assertThat(result.getCurrentLocks().get(0), is(sameInstance(existingLock.getCurrentLocks().get(0))));
        verify(lockInfoBroadcaster).broadcastLockUpdate(existingLock);
        verifyNoMoreInteractions(lockInfoBroadcaster);
    }


    
    @Test(expected = NullPointerException.class)
    public void test_unlockDocument_when_sessionNull_should_throwException() throws Exception {

        // DO THE ACTUAL CALL
        lockingServiceImpl.unlockDocument("45", null, "SessionID", LockLevel.DOCUMENT_LOCK);

    }

    @Test(expected = NullPointerException.class)
    public void test_unlockDocument_when_documentNull_should_throwException() throws Exception {

        // DO THE ACTUAL CALL
        lockingServiceImpl.unlockDocument(null, "userID","SessionID", LockLevel.DOCUMENT_LOCK);

    }

    @Test
    public void test_getLockingInfo_when_lock_should_returnLock() throws Exception {
        User user = ModelHelper.buildUser(33L, "userA", "user A");
        User userB = ModelHelper.buildUser(34L, "userB", "user B");

        LockActionInfo existingLockAR = lockingServiceImpl.lockDocument("45", user, "sessionID",LockLevel.READ_LOCK);
        LockActionInfo existingLockBR = lockingServiceImpl.lockDocument("45", userB, "sessionID2",LockLevel.READ_LOCK);
        LockActionInfo existingLockAD = lockingServiceImpl.lockDocument("45", user, "sessionID",LockLevel.DOCUMENT_LOCK);

        
        // DO THE ACTUAL CALL
        List<LockData> lockData = lockingServiceImpl.getLockingInfo("45");

        assertThat(lockData.size(), is(3));
        assertThat(lockData.get(0), is(sameInstance(existingLockAR.getCurrentLocks().get(0))));
        assertThat(lockData.get(1), is(sameInstance(existingLockBR.getCurrentLocks().get(1))));
        assertThat(lockData.get(2), is(sameInstance(existingLockAD.getCurrentLocks().get(2))));
    }

    @Test
    public void test_getLockingInfo_when_notLocked_should_returnEmpty() throws Exception {

        // DO THE ACTUAL CALL
    	List<LockData> lockData = lockingServiceImpl.getLockingInfo("45");

        assertThat(lockData, is(emptyCollectionOf(LockData.class)));
    }

    @Test(expected = NullPointerException.class)
    public void test_getLockingInfo_when_documentNull_should_throwException() throws Exception {

        // DO THE ACTUAL CALL
        lockingServiceImpl.getLockingInfo(null);

    }

    @Test
    public void test_getAllLocks_when_locksPresend_should_returnLocks() throws Exception {
        User user = ModelHelper.buildUser(33L, "userA", "user A");
        User userB = ModelHelper.buildUser(33L, "userA", "user B");
        LockActionInfo existingLock = lockingServiceImpl.lockDocument("45", user, "sessionID",LockLevel.READ_LOCK);
        LockActionInfo existingLock2 = lockingServiceImpl.lockDocument("451", user, "sessionID",LockLevel.READ_LOCK);
        LockActionInfo existingLock3 = lockingServiceImpl.lockDocument("8", userB, "sessionID",LockLevel.READ_LOCK);
        LockActionInfo existingLockD = lockingServiceImpl.lockDocument("45", user, "sessionID",LockLevel.DOCUMENT_LOCK);
        LockActionInfo existingLock2D = lockingServiceImpl.lockDocument("451", user, "sessionID",LockLevel.DOCUMENT_LOCK);
        LockActionInfo existingLock3D = lockingServiceImpl.lockDocument("8", userB, "sessionID",LockLevel.DOCUMENT_LOCK);
        
        
        // DO THE ACTUAL CALL
        ArrayList<LockData> lockInfoList= new ArrayList<LockData>();
         lockInfoList.addAll(lockingServiceImpl.getLockingInfo("45"));
         lockInfoList.addAll(lockingServiceImpl.getLockingInfo("451"));
         lockInfoList.addAll(lockingServiceImpl.getLockingInfo("8"));
        
        assertThat(lockInfoList.size(), is(6));
        assertThat(lockInfoList, hasItem(existingLock.getCurrentLocks().get(0)));
        assertThat(lockInfoList, hasItem(existingLock2.getCurrentLocks().get(0)));
        assertThat(lockInfoList, hasItem(existingLock3.getCurrentLocks().get(0)));
        assertThat(lockInfoList, hasItem(existingLockD.getCurrentLocks().get(1)));
        assertThat(lockInfoList, hasItem(existingLock2D.getCurrentLocks().get(1)));
        assertThat(lockInfoList, hasItem(existingLock3D.getCurrentLocks().get(1)));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getLockingInfo_when_tryToModifyList_should_throwError() throws Exception {

        User user = ModelHelper.buildUser(33L, "userA", "user A");
        LockActionInfo existingLock = lockingServiceImpl.lockDocument("45", user, "sessionID",LockLevel.READ_LOCK);

        // DO THE ACTUAL CALL
        Collection<LockData> lockInfoList = existingLock.getCurrentLocks();
        
        lockInfoList.remove(0);
    }

    @Test
    public void test_registerLockInfoBroadcastListener() {
        LockUpdateBroadcastListener lock = null;
        lockingServiceImpl.registerLockInfoBroadcastListener(lock);
        verify(lockInfoBroadcaster).register(lock);
    }

    @Test
    public void test_unregisterLockInfoBroadcastListener() {
        LockUpdateBroadcastListener lock = null;
        lockingServiceImpl.unregisterLockInfoBroadcastListener(lock);
        verify(lockInfoBroadcaster).unregister(lock);
    }

    @Test
    public void test_isDocumentLockedFor() throws Exception {
        User user = ModelHelper.buildUser(33L, "userA", "user A");
        LockActionInfo existingLockBR = lockingServiceImpl.lockDocument("45", user, "sessionID2",LockLevel.DOCUMENT_LOCK);
        
        //execute calls
        boolean lockPresent = lockingServiceImpl.isDocumentLockedFor("45", user.getLogin(), "sessionID2");
        boolean lockPresent1 = lockingServiceImpl.isDocumentLockedFor("45", user.getLogin(), "sessionID3");
        boolean lockPresent2 = lockingServiceImpl.isDocumentLockedFor("45", user.getLogin(), null);
        boolean lockPresent3 = lockingServiceImpl.isDocumentLockedFor("46", user.getLogin(), "SessionID4");
        boolean lockPresent4 = lockingServiceImpl.isDocumentLockedFor("45", "Login2", "SessionID4");
        
        //check
        assertThat(lockPresent, is(true));
        assertThat(lockPresent1, is(false));
        assertThat(lockPresent2, is(true));
        assertThat(lockPresent3, is(false));
    }
    
    @Test
    public void test_isElementLockedFor() throws Exception {
        User user = ModelHelper.buildUser(33L, "userA", "user A");
        LockActionInfo existingLockBR = lockingServiceImpl.lockDocument("45", user, "sessionID2",LockLevel.ELEMENT_LOCK, "E1");
        
        //execute calls
        boolean lockPresent = lockingServiceImpl.isElementLockedFor("45", user.getLogin(), "sessionID2", "E1");
        boolean lockPresent1 = lockingServiceImpl.isElementLockedFor("45", user.getLogin(), "sessionID3", "E1");
        boolean lockPresent2 = lockingServiceImpl.isElementLockedFor("45", user.getLogin(), null, "E1");
        boolean lockPresent3 = lockingServiceImpl.isElementLockedFor("45", user.getLogin(), "sessionID2", "E2");
        boolean lockPresent4 = lockingServiceImpl.isElementLockedFor("46", user.getLogin(), "sessionID2", "E1");
        boolean lockPresent5 = lockingServiceImpl.isElementLockedFor("46", "Login2", "sessionID2", "E1");
        
        //check
        assertThat(lockPresent, is(true));
        assertThat(lockPresent1, is(false));
        assertThat(lockPresent2, is(true));
        assertThat(lockPresent3, is(false));
        assertThat(lockPresent4, is(false));
        assertThat(lockPresent5, is(false));
        
    }
    
    @Test
    public void test_releaseLocksForSession() throws Exception{
        User user = ModelHelper.buildUser(33L, "userA", "user A");
        
        lockingServiceImpl.lockDocument("45", user, "sessionID2",LockLevel.READ_LOCK);
        lockingServiceImpl.lockDocument("45", user, "sessionID2",LockLevel.DOCUMENT_LOCK);
        lockingServiceImpl.lockDocument("45", user, "sessionID3",LockLevel.READ_LOCK);
        
        //execute
        LockActionInfo lockActionInfo = lockingServiceImpl.releaseLocksForSession("45", "sessionID2");

        //check
        List<LockData> lockData = lockActionInfo.getCurrentLocks();
        assertThat(lockActionInfo.sucesss(), is(true));
        assertThat(lockData.size(), is(1)); 
        
    }

    @Test
    public void test_unlockReadLock_when_DocLockPresent_should_releaseOnlyOneLock() throws Exception {
        User user = ModelHelper.buildUser(33L, "userA", "user A");
        User userB = ModelHelper.buildUser(34L, "userB", "user B");

        LockActionInfo existingLockAR = lockingServiceImpl.lockDocument("45", user, "sessionID",LockLevel.READ_LOCK);//to be released in test //1
        LockActionInfo existingLockAD = lockingServiceImpl.lockDocument("45", user, "sessionID",LockLevel.DOCUMENT_LOCK);//2
        LockActionInfo existingLockAR2 = lockingServiceImpl.lockDocument("45", user, "sessionID2",LockLevel.READ_LOCK);//3
        LockActionInfo existingLockBR = lockingServiceImpl.lockDocument("45", userB, "sessionID3",LockLevel.READ_LOCK);//4
        
        
        // DO THE ACTUAL CALL
        LockActionInfo remainingLockinfo = lockingServiceImpl.unlockDocument("45", user.getLogin(), "sessionID",LockLevel.READ_LOCK);//3

        //check 
        List<LockData> resultLockInfo=remainingLockinfo.getCurrentLocks();
        assertThat(resultLockInfo.size(), is(3)); //one lock released
        assertThat(resultLockInfo, hasItem(existingLockAD.getLock()));
        assertThat(resultLockInfo, hasItem(existingLockAR2.getLock()));
        assertThat(resultLockInfo, hasItem(existingLockBR.getLock()));

    }
    @Test
    public void test_unlockElementLock_when_twoElementLockPresent_should_releaseOnlyOneLock() throws Exception {
        User user = ModelHelper.buildUser(33L, "userA", "user A");
        User userB = ModelHelper.buildUser(34L, "userB", "user B");

        LockActionInfo existingLockAR = lockingServiceImpl.lockDocument("45", user, "sessionID",LockLevel.READ_LOCK);
        LockActionInfo existingLockAR2 =lockingServiceImpl.lockDocument("45", user, "sessionID2",LockLevel.READ_LOCK);
        LockActionInfo existingLockAA = lockingServiceImpl.lockDocument("45", user, "sessionID2",LockLevel.ELEMENT_LOCK, "E1");//to be released in test
        LockActionInfo existingLockAA2 = lockingServiceImpl.lockDocument("45", user, "sessionID",LockLevel.ELEMENT_LOCK, "E2");
        LockActionInfo existingLockBR = lockingServiceImpl.lockDocument("45", userB, "sessionID3",LockLevel.READ_LOCK);
        LockActionInfo existingLockBD = lockingServiceImpl.lockDocument("45", userB, "sessionID3",LockLevel.ELEMENT_LOCK,"E3");

        // DO THE ACTUAL CALL

        LockActionInfo remainingLockinfo = lockingServiceImpl.unlockDocument("45", user.getLogin(), "sessionID2",LockLevel.ELEMENT_LOCK,"E1");

        //check 
        List<LockData> resultLockInfo=remainingLockinfo.getCurrentLocks();
        
        assertThat(resultLockInfo.size(), is(5)); //one lock released
        assertThat(remainingLockinfo.sucesss(),is(true));
        assertThat(resultLockInfo, hasItem(existingLockAR.getCurrentLocks().get(0)));
        assertThat(resultLockInfo, hasItem(existingLockAR2.getCurrentLocks().get(1)));
      //assertThat(resultLockInfo, hasItem(existingLockAA.getLockInfo().get(2)));//removed by unlock
        assertThat(resultLockInfo, hasItem(existingLockAA2.getCurrentLocks().get(3)));
        assertThat(resultLockInfo, hasItem(existingLockBR.getCurrentLocks().get(4)));
        assertThat(resultLockInfo, hasItem(existingLockBD.getCurrentLocks().get(5)));
    }
    @Test
    public void test_unlockDocumentLock_when_twoReadLockPresent_should_releaseOnlyOneLock() throws Exception {
        User user = ModelHelper.buildUser(33L, "userA", "user A");
        User userB = ModelHelper.buildUser(34L, "userB", "user B");

        LockActionInfo existingLockAR = lockingServiceImpl.lockDocument("45", user, "sessionID",LockLevel.READ_LOCK);
        LockActionInfo existingLockAR2 =lockingServiceImpl.lockDocument("45", user, "sessionID2",LockLevel.READ_LOCK);
        LockActionInfo existingLockAD = lockingServiceImpl.lockDocument("45", user, "sessionID2",LockLevel.DOCUMENT_LOCK);//to be released in test
        LockActionInfo existingLockBR = lockingServiceImpl.lockDocument("45", userB, "sessionID3",LockLevel.READ_LOCK);

        // DO THE ACTUAL CALL

        LockActionInfo remainingLockinfo = lockingServiceImpl.unlockDocument("45", user.getLogin(), "sessionID2",LockLevel.DOCUMENT_LOCK);

        //check 
        List<LockData> resultLockInfo=remainingLockinfo.getCurrentLocks();
        
        assertThat(resultLockInfo.size(), is(3)); //one lock released
        
        assertThat(resultLockInfo, hasItem(existingLockAR.getCurrentLocks().get(0)));
        assertThat(resultLockInfo, hasItem(existingLockAR2.getCurrentLocks().get(1)));
      //assertThat(resultLockInfo, hasItem(existingLockAD.getLockInfo().get(2)));//removed by unlock
        assertThat(resultLockInfo, hasItem(existingLockBR.getCurrentLocks().get(3)));
    }
}
