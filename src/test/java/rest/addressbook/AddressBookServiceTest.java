package rest.addressbook;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.junit.After;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * A simple test suite
 *
 */
public class AddressBookServiceTest {

	HttpServer server;

	@Test
	public void serviceIsAlive() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		launchServer(ab);

		// Request the address book
		Client client = ClientBuilder.newClient();
		Response response = client.target("http://localhost:8282/contacts")
				.request().get();
		assertEquals(200, response.getStatus());
		assertEquals(0, response.readEntity(AddressBook.class).getPersonList()
				.size());

		//////////////////////////////////////////////////////////////////////
		// Verify that GET /contacts is well implemented by the service, i.e
		// test that it is safe and idempotent
		//////////////////////////////////////////////////////////////////////

		// Verifying that GET is safe and idempotent
		// Request the address book for second time
		// The two sequential requests, need to have the same response (safe and idempotent)
		Response response2 = client.target("http://localhost:8282/contacts")
				.request().get();
		// It has to return same response code (idempotent)
		assertEquals(200, response2.getStatus());
		// It has to return the same person list (as first request)
		assertEquals(0, response2.readEntity(AddressBook.class).getPersonList()
				.size());
	}

	@Test
	public void createUser() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		launchServer(ab);

		// Prepare data
		Person juan = new Person();
		juan.setName("Juan");
		URI juanURI = URI.create("http://localhost:8282/contacts/person/1");

		// Create a new user
		Client client = ClientBuilder.newClient();
		Response response = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(juan, MediaType.APPLICATION_JSON));

		assertEquals(201, response.getStatus());
		assertEquals(juanURI, response.getLocation());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person juanUpdated = response.readEntity(Person.class);
		assertEquals(juan.getName(), juanUpdated.getName());
		assertEquals(1, juanUpdated.getId());
		assertEquals(juanURI, juanUpdated.getHref());

		// Check that the new user exists
		response = client.target("http://localhost:8282/contacts/person/1")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		juanUpdated = response.readEntity(Person.class);
		assertEquals(juan.getName(), juanUpdated.getName());
		assertEquals(1, juanUpdated.getId());
		assertEquals(juanURI, juanUpdated.getHref());

		//////////////////////////////////////////////////////////////////////
		// Verify that POST /contacts is well implemented by the service, i.e
		// test that it is not safe and not idempotent
		//////////////////////////////////////////////////////////////////////	

		// Check if POST is safe (it's not)
		// Prepare data
		Person pedro = new Person();
		pedro.setName("Pedro");
		URI pedroURI = URI.create("http://localhost:8282/contacts/person/2");

		// Create a new user
		Client client2 = ClientBuilder.newClient();
		Response response2 = client2.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(pedro, MediaType.APPLICATION_JSON));

		assertEquals(201, response2.getStatus());
		assertEquals(pedroURI, response2.getLocation());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response2.getMediaType());
		Person pedroUpdated = response2.readEntity(Person.class);
		assertEquals(pedro.getName(), pedroUpdated.getName());
		assertEquals(2, pedroUpdated.getId());
		assertEquals(pedroURI, pedroUpdated.getHref());

		// Check that the new user exists
		response2 = client2.target("http://localhost:8282/contacts/person/2")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response2.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response2.getMediaType());
		pedroUpdated = response2.readEntity(Person.class);
		assertEquals(pedro.getName(), pedroUpdated.getName());
		assertEquals(2, pedroUpdated.getId());
		assertEquals(pedroURI, pedroUpdated.getHref());

		// Test if AddressBook has changed
		response = client.target("http://localhost:8282/contacts")
				.request().get();
		assertEquals(2,response.readEntity(AddressBook.class)
				.getPersonList().size());

		/*
			At this point, if this test has passed, it means that both POST
			request have modified the server resources, therefore, it's not safe.
		 */

		// Check if POST is idempotent (it's not)

		// Create existing user (repeat request)
		client = ClientBuilder.newClient();
		response = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(juan, MediaType.APPLICATION_JSON));

		assertEquals(201, response.getStatus());
		assertNotEquals(juanURI, response.getLocation());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		juanUpdated = response.readEntity(Person.class);
		assertEquals(juan.getName(), juanUpdated.getName());
		assertNotEquals(1, juanUpdated.getId());
		assertNotEquals(juanURI, juanUpdated.getHref());

		/*
			At this point, if this test has passed, it means that this repeated POST
			request have modified the server resources and have different outcome.
			Therefore, it's not idempotent (neither safe).
		 */
	}

	@Test
	public void createUsers() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		Person salvador = new Person();
		salvador.setName("Salvador");
		salvador.setId(ab.nextId());
		ab.getPersonList().add(salvador);
		launchServer(ab);

		// Prepare data
		Person juan = new Person();
		juan.setName("Juan");
		URI juanURI = URI.create("http://localhost:8282/contacts/person/2");
		Person maria = new Person();
		maria.setName("Maria");
		URI mariaURI = URI.create("http://localhost:8282/contacts/person/3");

		// Create a user
		Client client = ClientBuilder.newClient();
		Response response = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(juan, MediaType.APPLICATION_JSON));
		assertEquals(201, response.getStatus());
		assertEquals(juanURI, response.getLocation());

		// Create a second user
		response = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(maria, MediaType.APPLICATION_JSON));
		assertEquals(201, response.getStatus());
		assertEquals(mariaURI, response.getLocation());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person mariaUpdated = response.readEntity(Person.class);
		assertEquals(maria.getName(), mariaUpdated.getName());
		assertEquals(3, mariaUpdated.getId());
		assertEquals(mariaURI, mariaUpdated.getHref());

		// Check that the new user exists
		response = client.target("http://localhost:8282/contacts/person/3")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		mariaUpdated = response.readEntity(Person.class);
		assertEquals(maria.getName(), mariaUpdated.getName());
		assertEquals(3, mariaUpdated.getId());
		assertEquals(mariaURI, mariaUpdated.getHref());

		//////////////////////////////////////////////////////////////////////
		// Verify that GET /contacts/person/3 is well implemented by the service, i.e
		// test that it is safe and idempotent
		//////////////////////////////////////////////////////////////////////	

		// Verify that GET /contacts/person/3 is well implemented by the service
		response = client.target("http://localhost:8282/contacts/person/3")
				.request(MediaType.APPLICATION_JSON).get();
		// Response code is equal than first request
		assertEquals(200, response.getStatus());
		// MediaType is equal than first request
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		mariaUpdated = response.readEntity(Person.class);
		// Name is equal than original Person Object
		assertEquals(maria.getName(), mariaUpdated.getName());
		// ID is the same in both requests
		assertEquals(3, mariaUpdated.getId());
		// URI is the same in both requests
		assertEquals(mariaURI, mariaUpdated.getHref());
		// Test if AddressBook has changed
		response = client.target("http://localhost:8282/contacts")
				.request().get();
		assertEquals(3,response.readEntity(AddressBook.class)
				.getPersonList().size());

		/*
			At this point, if this test has passed, it means that this repeated GET
			request have not modified the server resources and have the same outcome.
			Therefore, it's safe and idempotent.
		 */
	}

	@Test
	public void listUsers() throws IOException {

		// Prepare server
		AddressBook ab = new AddressBook();
		Person salvador = new Person();
		salvador.setName("Salvador");
		// Added ID for Person
		salvador.setId(ab.nextId());
		Person juan = new Person();
		juan.setName("Juan");
		// Added ID for Person
		juan.setId(ab.nextId());
		ab.getPersonList().add(salvador);
		ab.getPersonList().add(juan);
		launchServer(ab);

		// Test list of contacts
		Client client = ClientBuilder.newClient();
		Response response = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		AddressBook addressBookRetrieved = response
				.readEntity(AddressBook.class);
		assertEquals(2, addressBookRetrieved.getPersonList().size());
		assertEquals(juan.getName(), addressBookRetrieved.getPersonList()
				.get(1).getName());

		//////////////////////////////////////////////////////////////////////
		// Verify that POST is well implemented by the service, i.e
		// test that it is not safe and not idempotent
		//////////////////////////////////////////////////////////////////////	
		// Check if POST is idempotent and safe (it's not)

		// Create existing users (repeat requests)
		URI salvadorURI = URI.create("http://localhost:8282/contacts/person/2");
		URI juanURI = URI.create("http://localhost:8282/contacts/person/1");

		// Insert "Juan" for second time
		juan = new Person();
		juan.setName("Juan");
		response = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(juan, MediaType.APPLICATION_JSON));

		assertEquals(201, response.getStatus());
		assertNotEquals(juanURI, response.getLocation());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person juanUpdated = response.readEntity(Person.class);
		assertEquals(juan.getName(), juanUpdated.getName());
		// Outcomes are not the same (not idempotent)
		assertNotEquals(1, juanUpdated.getId());
		assertNotEquals(juanURI, juanUpdated.getHref());

		// Insert "Salvador" for second time
		salvador = new Person();
		salvador.setName("Salvador");
		response = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(salvador, MediaType.APPLICATION_JSON));

		assertEquals(201, response.getStatus());
		assertNotEquals(salvadorURI, response.getLocation());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person salvadorUpdated = response.readEntity(Person.class);
		assertEquals(salvador.getName(), salvadorUpdated.getName());
		// Outcomes are not the same (not idempotent)
		assertNotEquals(2, salvadorUpdated.getId());
		assertNotEquals(salvadorURI, salvadorUpdated.getHref());

		// GET request for testing the POST request has created resources on the server
		// This means POST is not safe

		// Checking for the first POST
		juanURI = URI.create("http://localhost:8282/contacts/person/3");
		response = client.target("http://localhost:8282/contacts/person/3")
				.request(MediaType.APPLICATION_JSON).get();
		// Response code is equal than first request
		assertEquals(200, response.getStatus());
		// MediaType is equal than first request
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		juanUpdated = response.readEntity(Person.class);
		// Name is equal than original Person Object
		assertEquals(juan.getName(), juan.getName());
		// ID is the same in both requests
		assertEquals(3, juanUpdated.getId());
		// URI is the same in both requests
		assertEquals(juanURI, juanUpdated.getHref());

		// Checking for the second POST
		salvadorURI = URI.create("http://localhost:8282/contacts/person/4");
		response = client.target("http://localhost:8282/contacts/person/4")
				.request(MediaType.APPLICATION_JSON).get();
		// Response code is equal than first request
		assertEquals(200, response.getStatus());
		// MediaType is equal than first request
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		salvadorUpdated = response.readEntity(Person.class);
		// Name is equal than original Person Object
		assertEquals(salvador.getName(), salvador.getName());
		// ID is the same in both requests
		assertEquals(4, salvadorUpdated.getId());
		// URI is the same in both requests
		assertEquals(salvadorURI, salvadorUpdated.getHref());

		/*
			At this point, if this test has passed, it means that this repeated POST
			request have modified the server resources and have different outcome.
			Therefore, it's not idempotent (neither safe).
		 */
	}

	@Test
	public void updateUsers() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		Person salvador = new Person();
		salvador.setName("Salvador");
		salvador.setId(ab.nextId());
		Person juan = new Person();
		juan.setName("Juan");
		juan.setId(ab.getNextId());
		URI juanURI = URI.create("http://localhost:8282/contacts/person/2");
		ab.getPersonList().add(salvador);
		ab.getPersonList().add(juan);
		launchServer(ab);

		// Update Maria
		Person maria = new Person();
		maria.setName("Maria");
		Client client = ClientBuilder.newClient();
		Response response = client
				.target("http://localhost:8282/contacts/person/2")
				.request(MediaType.APPLICATION_JSON)
				.put(Entity.entity(maria, MediaType.APPLICATION_JSON));
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person juanUpdated = response.readEntity(Person.class);
		assertEquals(maria.getName(), juanUpdated.getName());
		assertEquals(2, juanUpdated.getId());
		assertEquals(juanURI, juanUpdated.getHref());

		// Verify that the update is real
		response = client.target("http://localhost:8282/contacts/person/2")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person mariaRetrieved = response.readEntity(Person.class);
		assertEquals(maria.getName(), mariaRetrieved.getName());
		assertEquals(2, mariaRetrieved.getId());
		assertEquals(juanURI, mariaRetrieved.getHref());

		// Verify that only can be updated existing values
		response = client.target("http://localhost:8282/contacts/person/3")
				.request(MediaType.APPLICATION_JSON)
				.put(Entity.entity(maria, MediaType.APPLICATION_JSON));
		assertEquals(400, response.getStatus());

		//////////////////////////////////////////////////////////////////////
		// Verify that PUT /contacts/person/2 is well implemented by the service, i.e
		// test that it is idempotent
		//////////////////////////////////////////////////////////////////////	

		// Test if PUT is idempotent (outcomes are the same, resources don't change)
		response = client
				.target("http://localhost:8282/contacts/person/2")
				.request(MediaType.APPLICATION_JSON)
				.put(Entity.entity(maria, MediaType.APPLICATION_JSON));
		// Test if response contains the request Person ("Maria") information
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		juanUpdated = response.readEntity(Person.class);
		assertEquals(maria.getName(), juanUpdated.getName());
		assertEquals(2, juanUpdated.getId());
		assertEquals(juanURI, juanUpdated.getHref());

		// Verifying the register for "Maria" has not changed in server
		response = client.target("http://localhost:8282/contacts/person/2")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		mariaRetrieved = response.readEntity(Person.class);
		assertEquals(maria.getName(), mariaRetrieved.getName());
		assertEquals(2, mariaRetrieved.getId());
		assertEquals(juanURI, mariaRetrieved.getHref());

		/*
			At this point, if this test has passed, it means that this repeated PUT
			request have not modified the server resources and have same outcome.
			Therefore, it's idempotent and not safe.
		 */
	}

	@Test
	public void deleteUsers() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		Person salvador = new Person();
		salvador.setName("Salvador");
		salvador.setId(1);
		Person juan = new Person();
		juan.setName("Juan");
		juan.setId(2);
		ab.getPersonList().add(salvador);
		ab.getPersonList().add(juan);
		launchServer(ab);

		// Delete a user
		Client client = ClientBuilder.newClient();
		Response response = client
				.target("http://localhost:8282/contacts/person/2").request()
				.delete();
		assertEquals(204, response.getStatus());

		// Verify that the user has been deleted
		response = client.target("http://localhost:8282/contacts/person/2")
				.request().delete();
		assertEquals(404, response.getStatus());

		//////////////////////////////////////////////////////////////////////
		// Verify that DELETE /contacts/person/2 is well implemented by the service, i.e
		// test that it is idempotent
		//////////////////////////////////////////////////////////////////////

		// Delete second user to test if DELETE is idempotent (already deleted)
		response = client.target("http://localhost:8282/contacts/person/2")
				.request().delete();
		assertEquals(404, response.getStatus());

		// Confirm the user has been deleted (already deleted)
		response = client.target("http://localhost:8282/contacts/person/2")
				.request().delete();
		assertEquals(404, response.getStatus());

		/*
			At this point, if this test has passed, it means that this repeated DELETE
			request have not modified the server resources and have same outcome (404).
			Therefore, it's idempotent and not safe.
		 */
	}

	@Test
	public void findUsers() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		Person salvador = new Person();
		salvador.setName("Salvador");
		salvador.setId(1);
		Person juan = new Person();
		juan.setName("Juan");
		juan.setId(2);
		ab.getPersonList().add(salvador);
		ab.getPersonList().add(juan);
		launchServer(ab);

		// Test user 1 exists
		Client client = ClientBuilder.newClient();
		Response response = client
				.target("http://localhost:8282/contacts/person/1")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person person = response.readEntity(Person.class);
		assertEquals(person.getName(), salvador.getName());
		assertEquals(person.getId(), salvador.getId());
		assertEquals(person.getHref(), salvador.getHref());

		// Test user 2 exists
		response = client.target("http://localhost:8282/contacts/person/2")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		person = response.readEntity(Person.class);
		assertEquals(person.getName(), juan.getName());
		assertEquals(2, juan.getId());
		assertEquals(person.getHref(), juan.getHref());

		// Test user 3 exists
		response = client.target("http://localhost:8282/contacts/person/3")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(404, response.getStatus());
	}

	private void launchServer(AddressBook ab) throws IOException {
		URI uri = UriBuilder.fromUri("http://localhost/").port(8282).build();
		server = GrizzlyHttpServerFactory.createHttpServer(uri,
				new ApplicationConfig(ab));
		server.start();
	}

	@After
	public void shutdown() {
		if (server != null) {
			server.shutdownNow();
		}
		server = null;
	}

}
