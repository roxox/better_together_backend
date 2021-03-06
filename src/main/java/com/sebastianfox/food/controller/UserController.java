package com.sebastianfox.food.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sebastianfox.food.models.*;
import com.sebastianfox.food.repository.Friendship.FriendshipRepository;
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
@Controller
@RequestMapping(path = "/api/user")
public class UserController {
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private Authenticator authenticator = new Authenticator();
    private ObjectMapper mapper = new ObjectMapper();
    private UserService userService = new UserService();

    // This means to get the bean called userRepository
    // Which is auto-generated by Spring, we will use it to handle the data
    @Autowired
    public UserController(UserRepository userRepository,
                          FriendshipRepository friendshipRepository) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
    }

    /* #############################
     *  Find USER(S) methods
     ############################# */

    /**
     * @param data/ JSON data from App
     * @return http response
     * @throws JSONException exception
     * @throws IOException   exception
     */
    @RequestMapping(path = "/findByUsername", method = RequestMethod.POST,
            consumes = {"application/json"})
    public ResponseEntity<Object> findByUsername(
            @RequestBody HashMap<String, String> data)
            throws JSONException, IOException {

        // get username from request
        String requestedUserName = data.get("userName");

        // check if username belongs to an existing user
        if (this.isUserNameAvailable(requestedUserName)) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        // Select user from database
        User requestedUser = this.findByUserName(requestedUserName);

        // Return to app
        return new ResponseEntity<>(createResposneJson("user", requestedUser), HttpStatus.OK);
    }

    /**
     * @param data JSON data from App
     * @return http response
     * @throws JSONException exception
     * @throws IOException   exception
     */
    @RequestMapping(path = "/findByMail", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> findByMail(@RequestBody HashMap<String, String> data)
            throws JSONException, IOException {

        // get email from request
        String email = data.get("email");

        // check if username belongs to an existing user
        if (this.isEmailAvailable(email)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // Select user from database
        User requestedUser = this.findByEmail(email);

        // Return to app
        return new ResponseEntity<>(createResposneJson("user", requestedUser), HttpStatus.OK);
    }

    /* #############################
     *  Register new User
     ############################# */

    /**
     * @param registerData JSON data from App
     * @return http response
     * @throws JSONException exception
     * @throws IOException   exception
     */
    @SuppressWarnings("Duplicates")
    @RequestMapping(path = "/registerUser", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> registerUser(@RequestBody HashMap<String, Object> registerData)
            throws JSONException, IOException {

        // Determine username, email and password from app request
        String username = (String) registerData.get("userName");
        String email = (String) registerData.get("email");
        String password = (String) registerData.get("password");

        // Check availability of Username
        if (userRepository.findByUserName(username) != null) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        // Check availability of Email
        if (userRepository.findByEmail(email) != null) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        User user = userService.createUser(username, email, password);
        userRepository.save(user);

        // Return to App
        return new ResponseEntity<>(createResposneJson("user", user), HttpStatus.CREATED);
    }

    /* #############################
     *  Authenticat User (1. without facebook, 2. with facebook)
     ############################# */

    /**
     * @param data JSON data from App
     * @return http response
     * @throws JSONException exception
     * @throws IOException   exception
     */
    @RequestMapping(path = "/authenticate", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> authenticate(@RequestBody HashMap<String, String> data)
            throws JSONException, IOException {

        User user = userRepository.findByEmail(data.get("email"));
        String password = data.get("password");

        // Failure at login (user not found or bad credentials)
        if (user == null || !authenticator.isExpectedPassword(password.toCharArray(), user.getSalt(), user.getPassword())) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        // Return to App
        return new ResponseEntity<>(createResposneJson("user", user), HttpStatus.ACCEPTED);
    }

    /**
     * @param data JSON data from App
     * @return http response
     * @throws JSONException exception
     * @throws IOException   exception
     */
    @SuppressWarnings("Duplicates")
    @RequestMapping(path = "/authenticateWithFacebook", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> authenticateWithFacebook(@RequestBody HashMap<String, Object> data)
            throws JSONException, IOException {

        // user rebuild from app request
        User requestedUser = mapper.convertValue(data.get("user"), User.class);

        // user from db find by facebook ID
        User facebookUser = userRepository.findByFacebookAccountId(requestedUser.getFacebookAccountId());

        if (facebookUser != null) {
            if (requestedUser.getId() == null || (!facebookUser.getId().equals(requestedUser.getId()))) {
                return new ResponseEntity<>(HttpStatus.CONFLICT);
            }
            if (!authenticator.isExpectedPassword(String.valueOf(facebookUser.getFacebookAccountId()).toCharArray(), facebookUser.getSalt(), facebookUser.getPassword())) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
            return new ResponseEntity<>(createResposneJson("user", facebookUser), HttpStatus.ACCEPTED);
        }

        if (requestedUser.getId() == null && requestedUser.getFacebookAccountEmail() != null) {
            userService.fullfilFacebookUser(requestedUser);
            userRepository.save(requestedUser);
            return new ResponseEntity<>(createResposneJson("user", requestedUser), HttpStatus.CREATED);
        }
        return new ResponseEntity<>(HttpStatus.CONFLICT);
    }

    /* #############################
     *  Modify and reload User
     ############################# */

    /**
     * @param data JSON data from App
     * @return http response
     * @throws JSONException exception
     * @throws IOException   exception
     */
    @SuppressWarnings("Duplicates")
    @RequestMapping(path = "/updateUser", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> updateUser(@RequestBody HashMap<String, User> data)
            throws JSONException, IOException {

        // Determine user from app request
        User appUser = data.get("user");

        // Fail (wrong format, User can not be determined)
        if (appUser == null || appUser.getId() == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // Load user from database
        User dbUser = userRepository.findById(appUser.getId());

        // Fail (user not found on db)
        if (dbUser == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // Check availability of Email (if email changed)
        if (!dbUser.getUserName().equals(appUser.getUserName())) {
            if (!isUserNameAvailable(appUser.getUserName())) {
                return new ResponseEntity<>(HttpStatus.CONFLICT);
            }
        }

        // Check availability of Email (if email changed)
        if (!dbUser.getEmail().equals(appUser.getEmail())) {
            if (!isEmailAvailable(appUser.getEmail())) {
                return new ResponseEntity<>(HttpStatus.CONFLICT);
            }
        }

        // Merge data from app user into db user
        dbUser.mergeDataFromOtherInstance(appUser);

        // save modified user
        userRepository.save(dbUser);

        // Return to App
        return new ResponseEntity<>(createResposneJson("user", dbUser), HttpStatus.OK);
    }

    /**
     * @param data JSON data from App
     * @return http response
     * @throws JSONException exception
     * @throws IOException   exception
     */
    @SuppressWarnings("Duplicates")
    @RequestMapping(path = "/reloadUser", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> reloadUser(@RequestBody HashMap<String, UUID> data)
            throws JSONException, IOException {

        User user = userRepository.findById(data.get("user_id"));

        // Fail
        if (user == null) {
            // Return to App
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // Return to App
        return new ResponseEntity<>(createResposneJson("user", user), HttpStatus.OK);
    }

    /* #############################
     *  Availability Checks
     ############################# */

    /**
     * @param registerData JSON data from App
     * @return http response
     * @throws JSONException exception
     */
    @SuppressWarnings("Duplicates")
    @RequestMapping(path = "/checkUsernameAvailability", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> checkUsernameAvailability(@RequestBody HashMap<String, String> registerData)
            throws JSONException {

        // Username already exist
        // Return to App
        if (this.isUserNameAvailable(registerData.get("username"))) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        // Username is available
        // Return to App
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * @param registerData JSON data from App
     * @return http response
     * @throws JSONException exception
     */
    @SuppressWarnings("Duplicates")
    @RequestMapping(path = "/checkEmailAvailability", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> checkMail(@RequestBody HashMap<String, String> registerData)
            throws JSONException {

        // Email already exist
        // Return to App
        if (this.isEmailAvailable(registerData.get("email"))) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        // Username is available
        // Return to App
        return new ResponseEntity<>(HttpStatus.OK);
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

    /* #############################
     *  Friendship handling
     ############################# */

    /**
     * @param data JSON data from App
     * @return http response
     * @throws JSONException exception
     * @throws IOException   exception
     */
    @RequestMapping(path = "/createAndAcceptFriendRequest", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> createAndAcceptFriendRequest(@RequestBody HashMap<String, UUID> data)
            throws JSONException, IOException {

        // get user_id from request
        User user = userRepository.findById(data.get("user_id"));

        // get friend_id from request
        User friend = userRepository.findById(data.get("friend_id"));

        // create friendship or accept friendship request (if existing) user -> friend
        Friendship friendship = user.createAndAcceptFriendship(friend);

        // save friendship
        friendshipRepository.save(friendship);

        // Return to app
        return new ResponseEntity<>(createResposneJson("user", user), HttpStatus.OK);
    }

    /**
     * @param data JSON data from App
     * @return http response
     * @throws JSONException exception
     * @throws IOException   exception
     */
    @RequestMapping(path = "/declineAndDeleteFriendship", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> declineAndDeleteFriendship(@RequestBody HashMap<String, UUID> data)
            throws JSONException, IOException {

        // get user_id from request
        User user = userRepository.findById(data.get("user_id"));

        // get friend_id from request
        User friend = userRepository.findById(data.get("friend_id"));

        // fremove friendship from friendship lists (user and friend)
        Friendship friendship = user.declineAndDeleteFriendship(friend);

        // remove freindship from database
        friendshipRepository.delete(friendship);

        // Return to app
        return new ResponseEntity<>(createResposneJson("user", user), HttpStatus.OK);
    }

    /**
     * @param key   for Response HashMap
     * @param value for Repsonse HashMao
     * @return String with ResponseJSON
     * @throws JSONException           exceptionhandling
     * @throws JsonProcessingException exceptionhandling
     */
    @SuppressWarnings("SameParameterValue")
    private String createResposneJson(String key, Object value) throws JSONException, JsonProcessingException {
        HashMap<String, Object> responseHash = new HashMap<>();
        responseHash.put(key, value);
        return mapper.writeValueAsString(responseHash);
    }
}