package com.sebastianfox.food.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sebastianfox.food.repository.EventRepository;
import com.sebastianfox.food.utils.Authenticator;
import com.sebastianfox.food.models.User;
import com.sebastianfox.food.repository.UserRepository;
import com.sebastianfox.food.utils.Debugger;
import com.sebastianfox.food.utils.Sha256Converter;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;

@SuppressWarnings("Duplicates")
@Controller    // This means that this class is a Controller
@RequestMapping(path = "/api/user") // This means URL's start with /api (after Application path)
public class UserController {
	private static final String USERNAMENOTAVAILABLE = "Benutzername bereits registriert";
	private static final String USERNOTFOUND = "User in nicht gefunden";
	private static final String EMAILNOTAVAILABLE = "Email Adresse bereits registriert";
	private static final String FACEBOOKUSERCREATED = "Facebook User wurde angelegt";
	private static final String BADCREDENTIALS = "Username oder Passwort falsch";
	private static final String FAILURE = "failure";
	private static final String SUCCESS = "success";

	private final UserRepository userRepository;
	private final EventRepository eventRepository;
	private Sha256Converter sessionGenerator = new Sha256Converter();

	private Authenticator authenticator = new Authenticator();

    // This means to get the bean called userRepository
    // Which is auto-generated by Spring, we will use it to handle the data
    @Autowired
    public UserController(UserRepository userRepository, EventRepository eventRepository) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

	@GetMapping(path = "/test2")
	public void testFunction2() {
		System.out.print("hallo");
	}

	/**
	 * register
	 * login
	 * fb login
	 * fb register
	 * get all user
	 * get by
	 *
	 * existing user request
	 */

	@GetMapping(path = "/test")
    public void testFunction(){

		User user = new User();
		user.setEmail("sebastian.fox@me.com");
		user.setUsername("basti1284");
		user.setSalt(authenticator.getNextSalt());
		user.setPassword(authenticator.hash("password".toCharArray(), user.getSalt()));
		userRepository.save(user);
		user.setSession(sessionGenerator.getSha256(user.getId().toString()));
		userRepository.save(user);



		User user2 = new User();
		user2.setEmail("sebastian.fox@rossox.com");
		user2.setUsername("sebastian.fox.12");
		user2.setSalt(authenticator.getNextSalt());
		user2.setPassword(authenticator.hash("password".toCharArray(), user2.getSalt()));
		userRepository.save(user2);
		user2.setSession(sessionGenerator.getSha256(user2.getId().toString()));
		user2.addFriend(user);
		userRepository.save(user2);

		System.out.println("User 1: ");
		for (User friend : user.getFriends()) {
		    System.out.println(friend.getUsername());
        }
		System.out.println("User 2: ");
        for (User friend : user2.getFriends()) {
            System.out.println(friend.getUsername());
        }

		/**
		MovieEvent movie = new MovieEvent();
		movie.setMovieTitle("Rush Hour");
        eventRepository.save(movie);

		FoodEvent food = new FoodEvent();
		food.setType("Hauptspeise");
		eventRepository.save(food);

		User user = new User();
		user.setEmail("sebastian.fox@me.com");
		user.setUsername("basti1284");
		user.setSalt(authenticator.getNextSalt());
		user.setPassword(authenticator.hash("password".toCharArray(), user.getSalt()));
		userRepository.save(user);
		user.setSession(sessionGenerator.getSha256(user.getId().toString()));
		userRepository.save(user);
        userRepository.save(user);

        User user2 = new User();
        user2.setEmail("sebastian.fox@icloud.com");
        user2.setUsername("basti12");
        user2.setSalt(authenticator.getNextSalt());
        user2.setPassword(authenticator.hash("password".toCharArray(), user2.getSalt()));
        userRepository.save(user2);
        user2.setSession(sessionGenerator.getSha256(user2.getId().toString()));
        userRepository.save(user2);

        user.addFriend(user2);
        userRepository.save(user);
	*/
	}

	/**
	 *
	 * @param loginData JSON data from App
	 * @return http response
	 * @throws JSONException exception
	 * @throws IOException exception
	 */
    @RequestMapping(path = "/authenticate", method = RequestMethod.POST, consumes = {"application/json"})
    public ResponseEntity<Object> authenticate(@RequestBody HashMap<String, String> loginData) throws JSONException, IOException {
		User user = userRepository.findByEmail(loginData.get("email"));
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String,HashMap> data = new HashMap<>();
		HashMap<String,Object> hashMap = new HashMap<>();

		// Failure at login (user not found or bad credentials)
		if (user == null || !authenticator.isExpectedPassword(loginData.get("password").toCharArray(), user.getSalt(), user.getPassword())) {
			hashMap.put("status", FAILURE);
			hashMap.put("message", BADCREDENTIALS);
			data.put("data", hashMap);
			// Object to JSON String
			String jsonString = mapper.writeValueAsString(hashMap);
			//User testUSer = mapper.readValue(jsonString, User.class);
			// Return to App
			return new ResponseEntity<>(jsonString, HttpStatus.CONFLICT);
		}

		// Successful login
		hashMap.put("session",user.getId().toString());
		hashMap.put("status","success");
		hashMap.put("user",user);
		hashMap.put("favorites", user.getFriends());
		data.put("data", hashMap);
		// Object to JSON String
		String jsonString = mapper.writeValueAsString(hashMap);
		// Return to App
		return new ResponseEntity<>(jsonString, HttpStatus.ACCEPTED);
	}

	/**
	 *
	 * @param userData JSON data from App
	 * @return http response
	 * @throws JSONException exception
	 * @throws IOException exception
	 */
	@SuppressWarnings("Duplicates")
	@RequestMapping(path = "/reloadUser", method = RequestMethod.POST, consumes = {"application/json"})
	public ResponseEntity<Object> reloadUser(@RequestBody HashMap<String, User> userData) throws JSONException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String,HashMap> data = new HashMap<>();
		HashMap<String,Object> hashMap = new HashMap<>();
		User appUser = userData.get("user");

		// check if user is available in database
		if (userRepository.findById(appUser.getId()) == null){
			hashMap.put("status", FAILURE);
			hashMap.put("message", USERNOTFOUND);
			//data.put("data", hashMap);
			// Object to JSON String
			String jsonString = mapper.writeValueAsString(hashMap);
			return new ResponseEntity<>(jsonString, HttpStatus.CONFLICT);
		}
		
		
		User user = userRepository.findById(appUser.getId());

		// Successful register
		hashMap.put("status","success");
		hashMap.put("user",user);
		//data.put("data", hashMap);
		// Object to JSON String
		String jsonString = mapper.writeValueAsString(hashMap);
		// Return to App
		return new ResponseEntity<>(jsonString, HttpStatus.ACCEPTED);
	}

	/**
	 *
	 * @param userData JSON data from App
	 * @return http response
	 * @throws JSONException exception
	 * @throws IOException exception
	 */
	@SuppressWarnings("Duplicates")
	@RequestMapping(path = "/updateUser", method = RequestMethod.POST, consumes = {"application/json"})
	public ResponseEntity<Object> updateUser(@RequestBody HashMap<String, User> userData) throws JSONException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String,HashMap> data = new HashMap<>();
		HashMap<String,Object> hashMap = new HashMap<>();
		User appUser = userData.get("user");
		User user = userRepository.findById(appUser.getId());
		user.mergeDataFromApp(appUser);
		userRepository.save(user);


		// Successful register
		hashMap.put("status","success");
		hashMap.put("user",user);
		data.put("data", hashMap);
		// Object to JSON String
		String jsonString = mapper.writeValueAsString(hashMap);
		// Return to App
		return new ResponseEntity<>(jsonString, HttpStatus.ACCEPTED);
	}

	/**
	 *
	 * @param registerData JSON data from App
	 * @return http response
	 * @throws JSONException exception
	 * @throws IOException exception
	 */
	@SuppressWarnings("Duplicates")
	@RequestMapping(path = "/checkUsername", method = RequestMethod.POST, consumes = {"application/json"})
	public ResponseEntity<Object> checkUsername(@RequestBody HashMap<String, String> registerData) throws JSONException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String,HashMap> data = new HashMap<>();
		HashMap<String,Object> hashMap = new HashMap<>();

		if (userRepository.findByUsername(registerData.get("username")) != null){
			hashMap.put("status", FAILURE);
			hashMap.put("message", USERNAMENOTAVAILABLE);
			data.put("data", hashMap);
			// Object to JSON String
			String jsonString = mapper.writeValueAsString(hashMap);
			return new ResponseEntity<>(jsonString, HttpStatus.CONFLICT);
		}

		// Successful register
		hashMap.put("status","success");
		data.put("data", hashMap);
		// Object to JSON String
		String jsonString = mapper.writeValueAsString(hashMap);
		// Return to App
		return new ResponseEntity<>(jsonString, HttpStatus.ACCEPTED);
	}

	/**
	 *
	 * @param registerData JSON data from App
	 * @return http response
	 * @throws JSONException exception
	 * @throws IOException exception
	 */
	@SuppressWarnings("Duplicates")
	@RequestMapping(path = "/checkMail", method = RequestMethod.POST, consumes = {"application/json"})
	public ResponseEntity<Object> checkMail(@RequestBody HashMap<String, String> registerData) throws JSONException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String,HashMap> data = new HashMap<>();
		HashMap<String,Object> hashMap = new HashMap<>();

		if (userRepository.findByEmail(registerData.get("mail")) != null){
			hashMap.put("status", FAILURE);
			hashMap.put("message", EMAILNOTAVAILABLE);
			data.put("data", hashMap);
			// Object to JSON String
			String jsonString = mapper.writeValueAsString(hashMap);
			return new ResponseEntity<>(jsonString, HttpStatus.CONFLICT);
		}

		// Successful register
		hashMap.put("status","success");
		data.put("data", hashMap);
		// Object to JSON String
		String jsonString = mapper.writeValueAsString(hashMap);
		// Return to App
		return new ResponseEntity<>(jsonString, HttpStatus.ACCEPTED);
	}

	/**
	 *
	 * @param registerData JSON data from App
	 * @return http response
	 * @throws JSONException exception
	 * @throws IOException exception
	 */
	@SuppressWarnings("Duplicates")
	@RequestMapping(path = "/registerUser", method = RequestMethod.POST, consumes = {"application/json"})
	public ResponseEntity<Object> registerUser(@RequestBody HashMap<String, String> registerData) throws JSONException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String,HashMap> data = new HashMap<>();
		HashMap<String,Object> hashMap = new HashMap<>();

		if (userRepository.findByUsername(registerData.get("username")) != null){
			hashMap.put("status", FAILURE);
			hashMap.put("message", USERNAMENOTAVAILABLE);
			data.put("data", hashMap);
			// Object to JSON String
			String jsonString = mapper.writeValueAsString(hashMap);
			return new ResponseEntity<>(jsonString, HttpStatus.CONFLICT);
		}
		if (userRepository.findByEmail(registerData.get("mail")) != null){
			hashMap.put("status", "failure");
			hashMap.put("message", EMAILNOTAVAILABLE);
			data.put("data", hashMap);
			// Object to JSON String
			String jsonString = mapper.writeValueAsString(hashMap);
			return new ResponseEntity<>(jsonString, HttpStatus.CONFLICT);
		}
		// Create and safe new user
		User user = new User();
		user.setUsername(registerData.get("username"));
		user.setEmail(registerData.get("mail"));
		user.setSalt(authenticator.getNextSalt());
		user.setPassword(authenticator.hash(registerData.get("password").toCharArray(), user.getSalt()));
		userRepository.save(user);

		// Successful register
		hashMap.put("status","success");
		hashMap.put("user", user);
		data.put("data", hashMap);
		// Object to JSON String
		String jsonString = mapper.writeValueAsString(hashMap);
		// Return to App
		return new ResponseEntity<>(jsonString, HttpStatus.ACCEPTED);
	}

	/**
	 *
	 * @param loginData JSON data from App
	 * @return http response
	 * @throws JSONException exception
	 * @throws IOException exception
	 */
	@SuppressWarnings("Duplicates")
	@RequestMapping(path = "/facebookLogin", method = RequestMethod.POST, consumes = {"application/json"})
	public ResponseEntity<Object> facebookLogin(@RequestBody HashMap<String, String> loginData) throws JSONException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String,HashMap> data = new HashMap<>();
		HashMap<String,Object> hashMap = new HashMap<>();
		User facebookUser = userRepository.findByFacebookMail(loginData.get("facebookMail"));
		if (facebookUser == null){
			Debugger.log(facebookUser);
			// Create and safe new user
			User user = new User();
			user.setUsername(loginData.get("facebookMail"));
			Debugger.log(loginData.get("facebookMail") + "for Username");
			user.setEmail(loginData.get("facebookMail"));
			Debugger.log(loginData.get("facebookMail") + "for Email");
			user.setFacebookMail(loginData.get("facebookMail"));
			Debugger.log(loginData.get("facebookMail") + "for Facebookmail");

			user.setSocialMediaAccount(true);

			userRepository.save(user);

			Debugger.log("User saved");
			hashMap.put("status", SUCCESS);
			hashMap.put("message", FACEBOOKUSERCREATED);
			hashMap.put("user", user);
			data.put("data", hashMap);
			// Object to JSON String
			String jsonString = mapper.writeValueAsString(hashMap);

			System.out.println("DEBUG: JSON String" + jsonString);
            return new ResponseEntity<>(jsonString, HttpStatus.ACCEPTED);
        }

		// Successful register
		hashMap.put("status",SUCCESS);
		hashMap.put("user", facebookUser);
		data.put("data", hashMap);
		// Object to JSON String
		String jsonString = mapper.writeValueAsString(hashMap);
		// Return to App
		return new ResponseEntity<>(jsonString, HttpStatus.ACCEPTED);
	}

	/**
	 *
	 * @param userData JSON data from App
	 * @return http response
	 * @throws JSONException exception
	 * @throws IOException exception
	 */
	/*
	@RequestMapping(path = "/getFriends", method = RequestMethod.POST, consumes = {"application/json"})
	public ResponseEntity<Object> getFriends(@RequestBody HashMap<String, String> userData) throws JSONException, IOException {
		User user = userRepository.findByUsername(userData.get("username"));
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String,HashMap> data = new HashMap<>();
		HashMap<String,Object> hashMap = new HashMap<>();
		//JSON from String to Object
		User user2 = mapper.readValue(userData.get("user"), User.class);

		// Failure at login (user not found or bad credentials)
		if (user == null) {
			hashMap.put("status", FAILURE);
			hashMap.put("message", BADCREDENTIALS);
			data.put("data", hashMap);
			// Object to JSON String
			String jsonString = mapper.writeValueAsString(hashMap);
			// Return to App
			return new ResponseEntity<>(jsonString, HttpStatus.CONFLICT);
		}

		// Successful login
		hashMap.put("status","success");
		hashMap.put("friends",user.getFriends());
		data.put("data", hashMap);
		// Object to JSON String
		String jsonString = mapper.writeValueAsString(hashMap);
		// Return to App
		return new ResponseEntity<>(jsonString, HttpStatus.ACCEPTED);
	}*/

	/**
	 * @param username JSON data from App
	 * @return http response
	 * @throws JSONException exception
	 * @throws IOException exception
	 */
	@RequestMapping(path = "/findByUsername", method = RequestMethod.POST, consumes = {"application/json"})
	public ResponseEntity<Object> findByUsername(@RequestBody HashMap<String, String> username) throws JSONException, IOException {
		User user = userRepository.findByUsername(username.get("username"));
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String,HashMap> data = new HashMap<>();
		HashMap<String,Object> hashMap = new HashMap<>();

		// Failure at login (user not found or bad credentials)
		if (user == null) {
			hashMap.put("status", FAILURE);
			hashMap.put("message", USERNOTFOUND);
			data.put("data", hashMap);
			// Object to JSON String
			String jsonString = mapper.writeValueAsString(hashMap);
			// Return to App
			return new ResponseEntity<>(jsonString, HttpStatus.CONFLICT);
		}

		hashMap.put("session",user.getId().toString());
		hashMap.put("status","success");
		hashMap.put("user",user);
		data.put("data", hashMap);
		// Object to JSON String
		String jsonString = mapper.writeValueAsString(hashMap);
		// Return to App
		return new ResponseEntity<>(jsonString, HttpStatus.ACCEPTED);
	}

	/**
	 *
	 * @param mail JSON data from App
	 * @return http response
	 * @throws JSONException exception
	 * @throws IOException exception
	 */
	@RequestMapping(path = "/findByMail", method = RequestMethod.POST, consumes = {"application/json"})
	public ResponseEntity<Object> findByMail(@RequestBody HashMap<String, String> mail) throws JSONException, IOException {
		User user = userRepository.findByEmail(mail.get("mail"));
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String,HashMap> data = new HashMap<>();
		HashMap<String,Object> hashMap = new HashMap<>();

		// Failure at login (user not found or bad credentials)
		if (user == null) {
			hashMap.put("status", FAILURE);
			hashMap.put("message", USERNOTFOUND);
			data.put("data", hashMap);
			// Object to JSON String
			String jsonString = mapper.writeValueAsString(hashMap);
			// Return to App
			return new ResponseEntity<>(jsonString, HttpStatus.CONFLICT);
		}

		hashMap.put("session",user.getId().toString());
		hashMap.put("status","success");
		hashMap.put("user",user);
		data.put("data", hashMap);
		// Object to JSON String
		String jsonString = mapper.writeValueAsString(hashMap);
		// Return to App
		return new ResponseEntity<>(jsonString, HttpStatus.ACCEPTED);
	}

	@GetMapping(path = "/getAllUsers")
	public @ResponseBody
	Iterable<User> getAllUsers() {
		// This returns a JSON or XML with the users
		return userRepository.findAll();
	}
}
