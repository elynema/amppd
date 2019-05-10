package edu.indiana.dlib.amppd.repository;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import edu.indiana.dlib.amppd.model.factory.ObjectFactory;
import edu.indiana.dlib.amppd.model.Collection;
import edu.indiana.dlib.amppd.model.Item;
import edu.indiana.dlib.amppd.model.Unit;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class CollectionRepositoryTests {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CollectionRepository collectionRepository;
	
	private ObjectFactory objFactory = new ObjectFactory();
	

	@Before 
	public void deleteAllBeforeTests() throws Exception {
		collectionRepository.deleteAll(); }


	@Test public void shouldReturnRepositoryIndex() throws Exception {
		mockMvc.perform(get("/")).andExpect(status().isOk()).
		andExpect( jsonPath("$._links.collections").exists()); }


	@Test public void shouldCreateCollection() throws Exception {
		mockMvc.perform(post("/collections").content(
				"{\"name\": \"Collection 1\", \"description\":\"For test\"}")).andExpect(
						status().isCreated()).andDo(MockMvcResultHandlers.print()).andExpect(
								header().string("Location", containsString("collections/"))); }

	@Test public void shouldRetrieveCollection() throws Exception {
		mockMvc.perform(post("/collections").
				content("{\"name\": \"Collection 1\", \"description\":\"For test\"}"))
		.andExpect(status().isCreated()).andReturn(); }


	@Test public void shouldQueryCollection() throws Exception {
		HashMap params = new HashMap<String, String>();
		Unit u = new Unit();
		u.setCollections(new ArrayList<Collection>());
		u.setName("Johnson");
		u.setDescription("for factories testing");
		
		params.put("unit", u);
		params.put("items", new ArrayList<Item>()); 
		
		Collection c = (Collection)objFactory.createDataEntityObject(params, "Collection");
		
		mockMvc.perform(post("/collections").content(
				"{ \"name\": \"121\", \"description\":\"For test\"}")).andDo(
						MockMvcResultHandlers.print()).andExpect( status().isCreated());

		mockMvc.perform(
				get("/collections/search/findByName?name=121")).andDo(MockMvcResultHandlers.
						print()).andExpect( status().isOk()).andExpect(
								jsonPath("$._embedded.collections[0].name").value( "121")) ; }


	@Test public void shouldUpdateCollection() throws Exception { MvcResult
		mvcResult = mockMvc.perform(post("/collections").content(
				"{\"name\": \"Collection 1\", \"description\":\"For test\"}")).andExpect(
						status().isCreated()).andReturn();

	String location = mvcResult.getResponse().getHeader("Location");

	mockMvc.perform(put(location).content(
			"{\"name\": \"Collection 1.1\", \"description\":\"For test\"}")).andExpect(
					status().isNoContent());

	mockMvc.perform(get(location)).andExpect(status().isOk()).andExpect(
			jsonPath("$.name").value("Collection 1.1")).andExpect(
					jsonPath("$.description").value("For test")); }



	@Test public void shouldPartiallyUpdateCollection() throws Exception {

		MvcResult mvcResult = mockMvc.perform(post("/collections").content(
				"{\"name\": \"Collection 1\", \"description\":\"For test\"}")).andExpect(
						status().isCreated()).andReturn();

		String location = mvcResult.getResponse().getHeader("Location");

		mockMvc.perform(
				patch(location).content("{\"name\": \"Collection 1.1.1\"}")).andExpect(
						status().isNoContent());

		mockMvc.perform(get(location)).andExpect(status().isOk()).andExpect(
				jsonPath("$.name").value("Collection 1.1.1")).andExpect(
						jsonPath("$.description").value("For test")); }

	
	@Test public void shouldDeleteCollection() throws Exception {

		MvcResult mvcResult = mockMvc.perform(post("/collections").content(
				"{ \"name\": \"Collection 1.1\", \"description\":\"For test\"}")).andExpect(
						status().isCreated()).andReturn();

		String location = mvcResult.getResponse().getHeader("Location");
		mockMvc.perform(delete(location)).andExpect(status().isNoContent());

		mockMvc.perform(get(location)).andExpect(status().isNotFound()); }


}
