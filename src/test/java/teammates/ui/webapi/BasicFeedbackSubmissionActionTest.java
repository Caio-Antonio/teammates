import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class FeedbackSessionAccessControlTest {

    private GateKeeper gateKeeper;
    private SQLLogic sqlLogic;

    @BeforeMethod
    public void setUp() {
        gateKeeper = new GateKeeper();
        sqlLogic = new SQLLogic();
    }

    private UserInfo mockUserInfo(String id) {
        return id == null ? null : new UserInfo(id);
    }

    private Student mockStudent(String googleId, boolean hasAccount) {
        Student student = new Student();
        if (hasAccount) {
            student.setAccount(new Account(googleId));
        }
        return student;
    }

    private FeedbackSession mockFeedbackSession(String courseId) {
        FeedbackSession feedbackSession = new FeedbackSession();
        feedbackSession.setCourse(new Course(courseId));
        return feedbackSession;
    }

    @DataProvider(name = "accessControlTestCases")
    public Object[][] provideTestCases() {
        return new Object[][]{
            {null, "F", "F", "F", null, null, false, "Trying to access system using a non-existent student entity"},
            {mockStudent(null, true), "V", "F", "F", mockUserInfo("instructor1"), "course1", false, null},
            {mockStudent("student1", true), "F", "V", "F", mockUserInfo("instructor1"), "course1", false, null},
            {mockStudent("student1", true), "F", "F", "F", null, "course1", false, "Login is required to access this feedback session"},
            {mockStudent(null, false), "F", "F", "V", mockUserInfo("student1"), "course1", false, null},
            {mockStudent("student1", true), "F", "F", "V", mockUserInfo("student1"), "course1", true, null},
        };
    }

    @Test(dataProvider = "accessControlTestCases")
    public void testAccessControl(
            Student student,
            String moderatedPerson,
            String previewAsPerson,
            String hasAccount,
            UserInfo userInfo,
            String courseId,
            boolean expectedAuthorized,
            String expectedExceptionMessage
    ) {
        FeedbackSession feedbackSession = mockFeedbackSession(courseId);

        try {
            if ("V".equals(moderatedPerson)) {
                Const.ParamsNames.FEEDBACK_SESSION_MODERATED_PERSON = "moderated";
            }
            if ("V".equals(previewAsPerson)) {
                Const.ParamsNames.PREVIEWAS = "preview";
            }

            new FeedbackSessionAccessControl(gateKeeper, sqlLogic).checkAccessControlForStudentFeedbackSubmission(student, feedbackSession);

            Assert.assertTrue(expectedAuthorized, "Expected the student to be authorized, but they were not.");
        } catch (UnauthorizedAccessException e) {
            Assert.assertEquals(e.getMessage(), expectedExceptionMessage);
        }
    }
}