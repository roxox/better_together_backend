package com.sebastianfox.food.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sebastianfox.food.Utils.Authenticator;
import com.sebastianfox.food.Utils.MultiAnswer;
import com.sebastianfox.food.entity.User;
import com.sebastianfox.food.repository.UserRepository;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;

@Controller    // This means that this class is a Controller
@RequestMapping(path = "/api") // This means URL's start with /api (after Application path)
public class UserController {
	private final UserRepository userRepository;

	private Authenticator authenticator = new Authenticator();

    // This means to get the bean called userRepository
    // Which is auto-generated by Spring, we will use it to handle the data
    @Autowired
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
	 * Checks if entered values are valid and create new user
	 *
	 * @param username username sent from app
	 * @param email email sent from app
	 * @param password password sent from app
	 * @param repeatedPassword repeatedPassword sent from app
	 * @return http response if the user authentication was successful
	 */
	@GetMapping(path = "/add") // Map ONLY GET Requests
	public @ResponseBody
    ResponseEntity<String> addNewUser(@RequestParam String username
			, @RequestParam String email, @RequestParam String password, @RequestParam String repeatedPassword) {
		// @ResponseBody means the returned String is the response, not a view name
		// @RequestParam means it is a parameter from the GET or POST request

		// check if transmitted values are valid
		if (username == null){
            return new ResponseEntity<>("EmptyUser", HttpStatus.CONFLICT);
		}
		if (email == null){
            return new ResponseEntity<>("EmptyMail", HttpStatus.CONFLICT);
		}
		if (password == null){
            return new ResponseEntity<>("EmptyPassword", HttpStatus.CONFLICT);
		}
		if (repeatedPassword == null){
            return new ResponseEntity<>("EmptyRepeatedPassword", HttpStatus.CONFLICT);
		}
		if (!password.equals(repeatedPassword)){
            return new ResponseEntity<>("PasswordVerificationFailure", HttpStatus.CONFLICT);
		}
		if (userRepository.findByUsername(username) != null) {
            return new ResponseEntity<>("DuplicatedUser", HttpStatus.CONFLICT);
		}
		if (userRepository.findByEmail(email) != null) {
            return new ResponseEntity<>("DuplicatedEmail", HttpStatus.CONFLICT);
		}

		// Create and safe new user
		User user = new User();
		user.setUsername(username);
		user.setEmail(email);
		user.setSalt(authenticator.getNextSalt());
		user.setPassword(authenticator.hash(password.toCharArray(), user.getSalt()));
		userRepository.save(user);
		//return "200 - saved";
        return new ResponseEntity<>("UserCreated", HttpStatus.ACCEPTED);
	}

	@GetMapping(path = "/getByMail")
	public @ResponseBody
	User getUserByMail(@RequestParam String email) {
		return userRepository.findByEmail(email);
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
		User user = userRepository.findByUsername(loginData.get("username"));
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String,HashMap> data = new HashMap<>();
		HashMap<String,Object> hashMap = new HashMap<>();

		// Failure at login (user not found or bad credentials)
		if (user == null || !authenticator.isExpectedPassword(loginData.get("password").toCharArray(), user.getSalt(), user.getPassword())) {
			hashMap.put("status", "failure");
			hashMap.put("message", "Username oder Passwort falsch");
			data.put("data", hashMap);
			// Object to JSON String
			String jsonString = mapper.writeValueAsString(data);
			// Return to App
			return new ResponseEntity<>(jsonString, HttpStatus.CONFLICT);
		}

		// Successful login
		hashMap.put("session",user.getId().toString());
		hashMap.put("status","success");
		hashMap.put("user",user);
		data.put("data", hashMap);
		// Object to JSON String
		String jsonString = mapper.writeValueAsString(data);
		// Return to App
		return new ResponseEntity<>(jsonString, HttpStatus.ACCEPTED);
	}

    enum SearchFields {
        USERNAME,
        FBUSERNAME,
        EMAIL,
        FBMAIL
    }

	private MultiAnswer elementExists(HashMap<String, String> receivedData, int seachfieldId) throws JsonProcessingException {
        HashMap<String,HashMap> data = new HashMap<>();
        HashMap<String,Object> hashMap = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        MultiAnswer answer = new MultiAnswer();
        answer.setStatus(false);
        answer.setJsonString("");
        SearchFields field = SearchFields.values()[seachfieldId];
        switch (field) {
            case USERNAME:
                if (userRepository.findByUsername(receivedData.get("username")) != null)//noinspection Duplicates
                {
                    hashMap.put("status", "failure");
                    hashMap.put("message", "Username bereits registriert");
                    data.put("data", hashMap);
                    // Object to JSON String
                    String jsonString = mapper.writeValueAsString(data);
                    answer.setStatus(true);
                    answer.setJsonString(jsonString);
                }
                break;
            case FBUSERNAME:
                if (userRepository.findByFbUsername(receivedData.get("username")) != null)//noinspection Duplicates
                {
                    hashMap.put("status", "failure");
                    hashMap.put("message", "Username bereits registriert");
                    data.put("data", hashMap);
                    // Object to JSON String
                    String jsonString = mapper.writeValueAsString(data);
                    answer.setStatus(true);
                    answer.setJsonString(jsonString);
                }
                break;
            case EMAIL:
                if (userRepository.findByEmail(receivedData.get("mail")) != null)//noinspection Duplicates
                {
                    hashMap.put("status", "failure");
                    hashMap.put("message", "Username bereits registriert");
                    data.put("data", hashMap);
                    // Object to JSON String
                    String jsonString = mapper.writeValueAsString(data);
                    answer.setStatus(true);
                    answer.setJsonString(jsonString);
                }
                break;
            case FBMAIL:
                if (userRepository.findByFbMail(receivedData.get("mail")) != null)//noinspection Duplicates
                {
                    hashMap.put("status", "failure");
                    hashMap.put("message", "Username bereits registriert");
                    data.put("data", hashMap);
                    // Object to JSON String
                    String jsonString = mapper.writeValueAsString(data);
                    answer.setStatus(true);
                    answer.setJsonString(jsonString);
                }
                break;
        }
        return answer;
    }

	/**
	 *
	 * @param registerData JSON data from App
	 * @return http response
	 * @throws JSONException exception
	 * @throws IOException exception
	 */
	@RequestMapping(path = "/registerUser", method = RequestMethod.POST, consumes = {"application/json"})
	public ResponseEntity<Object> registerUser(@RequestBody HashMap<String, String> registerData) throws JSONException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String,HashMap> data = new HashMap<>();
		HashMap<String,Object> hashMap = new HashMap<>();

        MultiAnswer userCheck = elementExists(registerData, 1);
        if (userCheck.isStatus()){
            return new ResponseEntity<>(userCheck.getJsonString(), HttpStatus.CONFLICT);
        }
        MultiAnswer emailCheck = elementExists(registerData, 3);
        if (emailCheck.isStatus()){
            return new ResponseEntity<>(userCheck.getJsonString(), HttpStatus.CONFLICT);
        }
        /*
        if (userRepository.findByUsername(registerData.get("username")) != null){
            hashMap.put("status", "failure");
            hashMap.put("message", "Username bereits registriert");
            data.put("data", hashMap);
            // Object to JSON String
            String jsonString = mapper.writeValueAsString(data);
            // Return to App
            return new ResponseEntity<Object>(jsonString, HttpStatus.CONFLICT);
        }
        if (userRepository.findByEmail(registerData.get("mail")) != null) {
            hashMap.put("status", "failure");
            hashMap.put("message", "Email bereits registriert");
            data.put("data", hashMap);
            // Object to JSON String
            String jsonString = mapper.writeValueAsString(data);
            // Return to App
            return new ResponseEntity<Object>(jsonString, HttpStatus.CONFLICT);
        }
        */
		// Create and safe new user
		User user = new User();
		user.setUsername(registerData.get("username"));
		user.setEmail(registerData.get("mail"));
		user.setSalt(authenticator.getNextSalt());
		user.setPassword(authenticator.hash(registerData.get("password").toCharArray(), user.getSalt()));
		userRepository.save(user);

		// Successful register
		hashMap.put("status","success");
		data.put("data", hashMap);
		// Object to JSON String
		String jsonString = mapper.writeValueAsString(data);
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
	@RequestMapping(path = "/registerFbUser", method = RequestMethod.POST, consumes = {"application/json"})
	public ResponseEntity<Object> registerFbUser(@RequestBody HashMap<String, String> registerData) throws JSONException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String,HashMap> data = new HashMap<>();
		HashMap<String,Object> hashMap = new HashMap<>();

		if (userRepository.findByUsername(registerData.get("username")) != null){
			hashMap.put("status", "failure");
			hashMap.put("message", "Username bereits registriert");
			data.put("data", hashMap);
			// Object to JSON String
			String jsonString = mapper.writeValueAsString(data);
			// Return to App
			return new ResponseEntity<>(jsonString, HttpStatus.CONFLICT);
		}
		if (userRepository.findByFbMail(registerData.get("fbMail")) != null) {
			hashMap.put("status", "failure");
			hashMap.put("message", "Email bereits registriert");
			data.put("data", hashMap);
			// Object to JSON String
			String jsonString = mapper.writeValueAsString(data);
			// Return to App
			return new ResponseEntity<>(jsonString, HttpStatus.CONFLICT);
		}

		// Create and safe new user
		User user = new User();
		user.setUsername(registerData.get("username"));
		user.setEmail(registerData.get("fbMail"));
		user.setFbMail(registerData.get("fbMail"));
		userRepository.save(user);

		// Successful register
		hashMap.put("status","success");
		data.put("data", hashMap);
		// Object to JSON String
		String jsonString = mapper.writeValueAsString(data);
		// Return to App
		return new ResponseEntity<>(jsonString, HttpStatus.ACCEPTED);
	}

	/**
	 *
	 * @param fbMail JSON data from App
	 * @return http response
	 * @throws JSONException exception
	 * @throws IOException exception
	 */
	@RequestMapping(path = "/findByFbMail", method = RequestMethod.POST, consumes = {"application/json"})
	public ResponseEntity<Object> findByFbMail(@RequestBody HashMap<String, String> fbMail) throws JSONException, IOException {
		User user = userRepository.findByUsername(fbMail.get("fbMail"));
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String,HashMap> data = new HashMap<>();
		HashMap<String,Object> hashMap = new HashMap<>();

		// Failure at login (user not found or bad credentials)
		if (user == null) {
			hashMap.put("status", "failure");
			hashMap.put("message", "User in Datenbank noch nicht vorhanden");
			data.put("data", hashMap);
			// Object to JSON String
			String jsonString = mapper.writeValueAsString(data);
			// Return to App
			return new ResponseEntity<>(jsonString, HttpStatus.CONFLICT);
		}

		// Successful login
		hashMap.put("session",user.getId().toString());
		hashMap.put("status","success");
		hashMap.put("user",user);
		data.put("data", hashMap);
		// Object to JSON String
		String jsonString = mapper.writeValueAsString(data);
		// Return to App
		return new ResponseEntity<>(jsonString, HttpStatus.ACCEPTED);
	}

	@GetMapping(path = "/all")
	public @ResponseBody
	Iterable<User> getAllUsers() {
		// This returns a JSON or XML with the users
		return userRepository.findAll();
	}
}
