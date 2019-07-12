package com.sebastianfox.food.controller;

//import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sebastianfox.food.daos.UserDao;
import com.sebastianfox.food.models.*;
import com.sebastianfox.food.repository.Event.EventRepository;
import com.sebastianfox.food.repository.Friendship.FriendshipRepository;
import com.sebastianfox.food.repository.Invitation.InvitationRepository;
import com.sebastianfox.food.services.UserService;
import com.sebastianfox.food.utils.Authenticator;
import com.sebastianfox.food.repository.User.UserRepository;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@SuppressWarnings("Duplicates")
@Controller    // This means that this class is a Controller
@RequestMapping(path = "/api/user")
// This means URL's start with /api (after Application path)
public class UserController {
    private final UserRepository userRepository;
    private final InvitationRepository invitationRepository;
    private final FriendshipRepository friendshipRepository;
    private final EventRepository eventRepository;
    private Authenticator authenticator = new Authenticator();
    private ObjectMapper mapper = new ObjectMapper();
    private UserDao userDao = new UserDao();
    private UserService userService = new UserService();

    // This means to get the bean called userRepository
    // Which is auto-generated by Spring, we will use it to handle the data
    @Autowired
    public UserController(UserRepository userRepository, InvitationRepository invitationRepository, FriendshipRepository friendshipRepository, EventRepository eventRepository) {
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
        this.friendshipRepository = friendshipRepository;
        this.eventRepository = eventRepository;
    }

    /*
     * register
     * login
     * fb login
     * fb register
     * get all user
     * get by
     * <p>
     * existing user request
     */

    /**
     * @param data JSON data from App
     * @return http response
     * @throws JSONException exception
     * @throws IOException   exception
     */
    @RequestMapping(path = "/authenticate", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> authenticate(@RequestBody HashMap<String, String> data) throws JSONException, IOException {

        User user = userRepository.findByEmail(data.get("email"));
        String password = data.get("password");

        HashMap<String, Object> responseHash = new HashMap<>();

        // Failure at login (user not found or bad credentials)
        if (user == null || !authenticator.isExpectedPassword(password.toCharArray(), user.getSalt(), user.getPassword())) {
            // Object  to JSON String
            String jsonString = mapper.writeValueAsString(responseHash);
            //User testUSer = mapper.readValue(jsonString, User.class);
            // Return to App
            return new ResponseEntity<>(jsonString, HttpStatus.UNAUTHORIZED);
        }

        // Successful login
        responseHash.put("user", user);
        // Object to JSON String
        String responseJson = mapper.writeValueAsString(responseHash);
        // Return to App
        return new ResponseEntity<>(responseJson, HttpStatus.OK);
    }

    /**
     * @param data JSON data from App
     * @return http response
     * @throws JSONException exception
     * @throws IOException   exception
     */
    @SuppressWarnings("Duplicates")
    @RequestMapping(path = "/reloadUser", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> reloadUser(@RequestBody HashMap<String, UUID> data) throws JSONException, IOException {

        HashMap<String, Object> responseHash = new HashMap<>();
        User user = userRepository.findById(data.get("id"));

        // Fail
        if (user == null) {
            // Object to JSON String
            String jsonString = mapper.writeValueAsString(responseHash);
            // Return to App
            return new ResponseEntity<>(jsonString, HttpStatus.NOT_FOUND);
        }

        // Success
        responseHash.put("user", user);
        // Object to JSON String
        String responseJson = mapper.writeValueAsString(responseHash);
        // Return to App
        return new ResponseEntity<>(responseJson, HttpStatus.OK);
    }

    /**
     * @param userData JSON data from App
     * @return http response
     * @throws JSONException exception
     * @throws IOException   exception
     */
    @SuppressWarnings("Duplicates")
    @RequestMapping(path = "/updateUser", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> updateUser(@RequestBody HashMap<String, User> userData) throws JSONException, IOException {

        HashMap<String, Object> responseHash = new HashMap<>();

        // Determine user from app request
        User appUser = userData.get("user");

        // Fail
        if (appUser == null || appUser.getId() == null) {
            // Object to JSON String
            String jsonString = mapper.writeValueAsString(responseHash);
            // Return to App
            return new ResponseEntity<>(jsonString, HttpStatus.NOT_FOUND);
        }

        // Load user from database
        User dbUser = userRepository.findById(appUser.getId());

        // Fail
        if (dbUser == null) {
            // Object to JSON String
            String jsonString = mapper.writeValueAsString(responseHash);
            // Return to App
            return new ResponseEntity<>(jsonString, HttpStatus.NOT_FOUND);
        }

        // Merge data from app user into db user
        dbUser.mergeDataFromOtherInstance(appUser);
        userRepository.save(dbUser);

        // Successful register
        responseHash.put("user", dbUser);
        // Object to JSON String
        String responseJson = mapper.writeValueAsString(responseHash);
        // Return to App
        return new ResponseEntity<>(responseJson, HttpStatus.OK);
    }

    /**
     * @param registerData JSON data from App
     * @return http response
     * @throws JSONException exception
     */
    @SuppressWarnings("Duplicates")
    @RequestMapping(path = "/checkUsernameAvailability", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> checkUsernameAvailability(@RequestBody HashMap<String, String> registerData) throws JSONException {

        // Username already exist
        // Return to App
        if (this.isUserNameAvailable(registerData.get("username"))) {
            return new ResponseEntity<>(null, HttpStatus.CONFLICT);
        }

        // Username is available
        // Return to App
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    /**
     * @param registerData JSON data from App
     * @return http response
     * @throws JSONException exception
     */
    @SuppressWarnings("Duplicates")
    @RequestMapping(path = "/checkEmailAvailability", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> checkMail(@RequestBody HashMap<String, String> registerData) throws JSONException {

        // Email already exist
        // Return to App
        if (this.isEmailAvailable(registerData.get("email"))) {
            return new ResponseEntity<>(null, HttpStatus.CONFLICT);
        }

        // Username is available
        // Return to App
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    /**
     * @param registerData JSON data from App
     * @return http response
     * @throws JSONException exception
     * @throws IOException   exception
     */
    @SuppressWarnings("Duplicates")
    @RequestMapping(path = "/registerUser", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> registerUser(@RequestBody HashMap<String, Object> registerData) throws JSONException, IOException {

        // Determine username, email and password from app request
        String username = (String) registerData.get("userName");
        String email = (String) registerData.get("email");
        String password = (String) registerData.get("password");

        HashMap<String, Object> responseHash = new HashMap<>();

        // Check availability of Username
        if (userRepository.findByUserName(username) != null) {
            // Object to JSON String
            return new ResponseEntity<>(null, HttpStatus.CONFLICT);
        }

        // Check availability of Email
        if (userRepository.findByEmail(email) != null) {
            return new ResponseEntity<>(null, HttpStatus.CONFLICT);
        }

        User user = userService.createUser(username, email, password);
        userRepository.save(user);

        // Successful register
        responseHash.put("user", user);
        // Object to JSON String
        String responseJson = mapper.writeValueAsString(responseHash);
        // Return to App
        return new ResponseEntity<>(responseJson, HttpStatus.CREATED);
    }

    /**
     * @param loginData JSON data from App
     * @return http response
     * @throws JSONException exception
     * @throws IOException   exception
     */
    @SuppressWarnings("Duplicates")
    @RequestMapping(path = "/facebookLogin", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> facebookLogin(@RequestBody HashMap<String, Object> loginData) throws JSONException, IOException {

        HashMap<String, Object> hashMap = new HashMap<>();
        User requestedUser = (User) loginData.get("user");
        User facebookUser = userRepository.findByFacebookAccountId(requestedUser.getFacebookAccountId());

        // If user does not exist, create it
        if (facebookUser == null) {
            userRepository.save(requestedUser);

            Iterable<Invitation> invitations = invitationRepository.findByEmail(requestedUser.getEmail());
            for (Invitation invitation : invitations) {
                invitation.setInvited(requestedUser);
                invitationRepository.save(invitation);
            }


            hashMap.put("user", requestedUser);
            // Object to JSON String
            String jsonString = mapper.writeValueAsString(hashMap);
            return new ResponseEntity<>(jsonString, HttpStatus.CREATED);
        }

        // User already exists and is foud in database
        hashMap.put("user", facebookUser);
        // Object to JSON String
        String jsonString = mapper.writeValueAsString(hashMap);
        // Return to App
        return new ResponseEntity<>(jsonString, HttpStatus.OK);
    }

    /**
     * @param data/ JSON data from App
     * @return http response
     * @throws JSONException exception
     * @throws IOException   exception
     */
    @RequestMapping(path = "/findByUsername", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> findByUsername(@RequestBody HashMap<String, String> data) throws JSONException, IOException {

        // create hashmap for response
        HashMap<String, Object> responseHash = new HashMap<>();

        // get username from request
        String requestedUserName = data.get("userName");

        // check if username belongs to an existing user
        if (this.isUserNameAvailable(requestedUserName)){

            // no user with requested name found, return back to app
           return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        // Select user from database
        User requestedUser = this.findByUserName(requestedUserName);

        // collect/prepare response data
        responseHash.put("user", requestedUser);

        // object to JSON String
        String jsonString = mapper.writeValueAsString(responseHash);

        // Return to app
        return new ResponseEntity<>(jsonString, HttpStatus.OK);
    }

    /**
     * @param data JSON data from App
     * @return http response
     * @throws JSONException exception
     * @throws IOException   exception
     */
    @RequestMapping(path = "/findByMail", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> findByMail(@RequestBody HashMap<String, String> data) throws JSONException, IOException {

        // create hashmap for response
        HashMap<String, Object> responseHash = new HashMap<>();

        // get email from request
        String requestedEmail = data.get("email");

        // check if username belongs to an existing user
        if (this.isEmailAvailable(requestedEmail)){

            // no user with requested name found, return back to app
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        // Select user from database
        User requestedUser = this.findByEmail(requestedEmail);

        // collect/prepare response data
        responseHash.put("user", requestedUser);

        // object to JSON String
        String jsonString = mapper.writeValueAsString(responseHash);

        // Return to app
        return new ResponseEntity<>(jsonString, HttpStatus.OK);
    }

    /*
        Friendship handling
     */

    /**
     * @param data JSON data from App
     * @return http response
     * @throws JSONException exception
     * @throws IOException   exception
     */
    @RequestMapping(path = "/createAndAcceptFriendRequest", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> createAndAcceptFriendRequest(@RequestBody HashMap<String, UUID> data) throws JSONException, IOException {

        // create hashmap for response
        HashMap<String, Object> responseHash = new HashMap<>();

        // get user_id from request
        User user = userRepository.findById(data.get("user_id"));

        // get friend_id from request
        User friend = userRepository.findById(data.get("friend_id"));

        // create friendship or accept friendship request (if existing) user -> friend
        Friendship friendship = user.createAndAcceptFriendship(friend);

        // save friendship
        friendshipRepository.save(friendship);

        // collect/prepare response data (return user with current friendships)
        responseHash.put("user", user);

        // Object to JSON String
        String jsonString = mapper.writeValueAsString(responseHash);

        // Return to app
        return new ResponseEntity<>(jsonString, HttpStatus.OK);
    }

    /**
     * @param data JSON data from App
     * @return http response
     * @throws JSONException exception
     * @throws IOException   exception
     */
    @RequestMapping(path = "/declineAndDeleteFriendship", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> declineAndDeleteFriendship(@RequestBody HashMap<String, UUID> data) throws JSONException, IOException {

        // create hashmap for response
        HashMap<String, Object> responseHash = new HashMap<>();

        // get user_id from request
        User user = userRepository.findById(data.get("user_id"));

        // get friend_id from request
        User friend = userRepository.findById(data.get("friend_id"));

        // fremove friendship from friendship lists (user and friend)
        Friendship friendship = user.declineAndDeleteFriendship(friend);

        // remove freindship from database
        friendshipRepository.delete(friendship);

        // collect/prepare response data (return user with current friendships)
        responseHash.put("user", user);

        // Object to JSON String
        String jsonString = mapper.writeValueAsString(responseHash);

        // Return to app
        return new ResponseEntity<>(jsonString, HttpStatus.OK);
    }

    /**
     * @param email JSON data from App
     * @return boolean
     */
    private boolean isEmailAvailable(String email) {

        // Mail already exist
        return userRepository.findByEmail(email) == null;
    }

    /**
     * @param userName JSON data from App
     * @return boolean
     */
    private boolean isUserNameAvailable(String userName) {

        // UserName already exist
        return userRepository.findByUserName(userName) == null;
    }
    /**
     * @param email JSON data from App
     * @return boolean
     */
    private User findByEmail(String email) {

        // Get user by its (distinct) email address
        return userRepository.findByEmail(email);
    }

    /**
     * @param userName JSON data from App
     * @return boolean
     */
    private User findByUserName(String userName) {

        // Get user by its (distinct) userName
        return userRepository.findByUserName(userName);
    }

    /**
     * @param data JSON data from App
     * @return http response
     * @throws JSONException exception
     */
    @SuppressWarnings("Duplicates")
    @RequestMapping(path = "/updateUserEmail", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> updateUserEmail(@RequestBody HashMap<String,HashMap> data) throws JSONException, IOException {

        // create hashmap for response
        HashMap<String, Object> responseHash = new HashMap<>();

        // exclude data from hashmap
        HashMap dataHashMap = data.get("data");

        // create JSON string from sub hashmap with user data
        String userJsonString = mapper.writeValueAsString(dataHashMap.get("user"));

        // transform JSON to User
        User user = mapper.readValue(userJsonString, User.class);

        // get user from db
        User dbUser = userRepository.findById(user.getId());

        // get new email from request and set it on database user
        if (isEmailAvailable(user.getEmail())) {

            // set new email address
            dbUser.setEmail(user.getEmail());

            // sace user
            userRepository.save(dbUser);

            // collect/prepare response data (return user with current friendships)
            responseHash.put("user", dbUser);

            // Object to JSON String
            String jsonString = mapper.writeValueAsString(responseHash);

            // Return to app (successful)
            return new ResponseEntity<>(jsonString, HttpStatus.OK);
        }

        // Object to JSON String
        responseHash.put("user", dbUser);

        // Object to JSON String
        String jsonString = mapper.writeValueAsString(responseHash);

        // Return to app (not successful)
        return new ResponseEntity<>(jsonString, HttpStatus.CONFLICT);
    }


    /**
     * @param data JSON data from App
     * @return http response
     * @throws JSONException exception
     * @throws IOException   exception
     */
    @RequestMapping(path = "/testEventGetter", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> testEventGetter(@RequestBody HashMap<String, UUID> data) throws JSONException, IOException {

        // create hashmap for response
        HashMap<String, Object> responseHash = new HashMap<>();

        // get user_id from request
        User user = userRepository.findById(data.get("user_id"));

        List<Event> events = new ArrayList<>(user.getEventsOfAllConnections());
        System.out.println("test");
        return null;
    }

    /**
     *
     * @return http response
     * @throws JSONException exception
     * @throws IOException exception
     */
    @RequestMapping(path = "/createTestData", method = RequestMethod.POST, consumes = {"application/json"})
    public void createTestData(@RequestBody HashMap<String, Integer> data) throws JSONException, IOException {


//        ############################
//        WITH  Hibernate START
//        ############################

//        Long test = userRepository.count();
        Long numberOfExistingTestEntities = userRepository.countUsers();
        int numberOfNewTestEntities = data.get("number");

        for ( Long i = numberOfExistingTestEntities; i < numberOfNewTestEntities+numberOfExistingTestEntities; i++ ) {
            User user = new User();
            user.setUserName("Testuser".concat(Long.toString(i)));
//            user.setUserName("Testuser");
            user.setSalt(authenticator.getNextSalt());
            user.setPassword(authenticator.hash("password".toCharArray(), user.getSalt()));
            user.setEmail("testmail".concat(Long.toString(i)).concat("@mail.de"));
            userRepository.save(user);
        }

//        ############################
//        WITH  Hibernate END
//        ############################

//        Event event1 = new Event();
//        event1.setOwner(user1);
//        event1.setText("Beachvolleyball");
////        event1.setDate(new Date("31.07.2019"));
////        eventRepository.save(event1);

        Iterable<User> users = userRepository.findAll();
        Iterable<User> users2 = userDao.getUsers();
        Iterable<Event> events = eventRepository.findAll();

        System.out.println("\n#####################################");
        System.out.println("List of available Users (Testdata)");
        System.out.println("#####################################");
        for (User user : users) {
            System.out.println(user.getId().toString());
        }


//        System.out.println("\n#####################################");
//        System.out.println("Test Custom Repository (+ Interface)");
//        System.out.println("#####################################");
//        List<User> users3 = userRepository.findUserByFancyStuff("testmail13@mail.de");
//
//
//
//        System.out.println("\n#####################################");
//        System.out.println("Test Custom Repository JQL");
//        System.out.println("#####################################");
//        User user = userRepository.findUserByMyFancyMail("testmail13@mail.de");
//        System.out.println("test startet hier");
//
//        System.out.println("direkt nach dem laden");
//        System.out.println(user.getUserName());
//
//        System.out.println("nach void Methode");
//        userService.doSomeFancyUserStuffWithoutReturn(user);
//        System.out.println(user.getUserName());
//        userRepository.save(user);
//
//        System.out.println("nach return Methode");
//        user = userService.doSomeFancyUserStuff(user);
//        System.out.println(user.getUserName());
//        userRepository.save(user);
//
//        System.out.println("test endet hier");
//
//
//        System.out.println("#####################################");


//        ############################
//        WITH  persistence_old.xml START
//        ############################

//        EntityManagerFactory emfactory = Persistence.
//                createEntityManagerFactory( "plasma.persistence" );
//        EntityManager entitymanager = emfactory.
//                createEntityManager( );
//        CriteriaBuilder criteriaBuilder = entitymanager
//                .getCriteriaBuilder();
//        CriteriaQuery<Object> criteriaQuery = criteriaBuilder
//                .createQuery();
//        Root<User> from = criteriaQuery.from(User.class);
//
//        //select all records
//        System.out.println("Select all records");
//        CriteriaQuery<Object> select =criteriaQuery.select(from);
//        TypedQuery<Object> typedQuery = entitymanager
//                .createQuery(select);
//        List<Object> resultlist= typedQuery.getResultList();
//

//        ############################
//        WITH  persistence_old.xml  END
//        ############################


//        System.out.println("\n#####################################");
//        System.out.println("List of available Events (Testdata)");
//        System.out.println("#####################################");
//        for (Event event : events) {
//            System.out.println(event.getId().toString().concat(": ").concat(event.getText()));
//        }



    }



}
