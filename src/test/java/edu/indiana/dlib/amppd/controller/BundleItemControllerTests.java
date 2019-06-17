package edu.indiana.dlib.amppd.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import edu.indiana.dlib.amppd.model.Bundle;
import edu.indiana.dlib.amppd.model.Item;
import edu.indiana.dlib.amppd.repository.BundleRepository;
import edu.indiana.dlib.amppd.repository.ItemRepository;
import lombok.extern.java.Log;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
@Log
public class BundleItemControllerTests {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private BundleRepository bundleRepository;
	
    @MockBean
    private ItemRepository itemRepository;
	
	// TODO: verify redirect for all following tests
	
    @Test
    public void shouldAddItemToBundle() throws Exception {
    	Item item = new Item();
    	item.setId(1l);
    	item.setBundles(new HashSet<Bundle>());
    	
    	Bundle bundle = new Bundle();
    	bundle.setId(1l);
    	bundle.setItems(new HashSet<Item>());
    	
    	Mockito.when(itemRepository.findById(1l)).thenReturn(Optional.of(item)); 
    	Mockito.when(itemRepository.save(item)).thenReturn(item); 
    	Mockito.when(bundleRepository.findById(1l)).thenReturn(Optional.of(bundle)); 
    	Mockito.when(bundleRepository.save(bundle)).thenReturn(bundle); 
    	
    	mvc.perform(post("/bundles/1/add/items/1")).andExpect(status().isOk()).andExpect(
//				jsonPath("$.items", hasSize(1))).andExpect(	// TODO need to import org.hamcrest.Matchers.hasSize with added dependency hamcrest-all
						jsonPath("$.items[0].id").value(1));
    }

    @Test
    public void shouldDeleteItemFromBundle() throws Exception {   	
    	Item item = new Item();
    	item.setId(1l);
    	item.setBundles(new HashSet<Bundle>());
    	
    	Bundle bundle = new Bundle();
    	bundle.setId(1l);
    	bundle.setItems(new HashSet<Item>());
    	
    	item.getBundles().add(bundle);
    	bundle.getItems().add(item);    	
    	
    	Mockito.when(itemRepository.findById(1l)).thenReturn(Optional.of(item)); 
    	Mockito.when(itemRepository.save(item)).thenReturn(item); 
    	Mockito.when(bundleRepository.findById(1l)).thenReturn(Optional.of(bundle)); 
    	Mockito.when(bundleRepository.save(bundle)).thenReturn(bundle); 
    	
    	mvc.perform(post("/bundles/1/delete/items/1")).andExpect(status().isOk()).andExpect(
//				jsonPath("$.items", hasSize(0))).andExpect( 
						jsonPath("$.items[0].id").doesNotExist());    	
    }

    @Test
    public void shouldNotAddDuplicateItemToBundle() throws Exception {   	
    	Item item = new Item();
    	item.setId(1l);
    	item.setBundles(new HashSet<Bundle>());
    	
    	Bundle bundle = new Bundle();
    	bundle.setId(1l);
    	bundle.setItems(new HashSet<Item>());
    	
    	item.getBundles().add(bundle);
    	bundle.getItems().add(item);    	
    	
    	Mockito.when(itemRepository.findById(1l)).thenReturn(Optional.of(item)); 
//    	Mockito.when(itemRepository.save(item)).thenReturn(item); 
    	Mockito.when(bundleRepository.findById(1l)).thenReturn(Optional.of(bundle)); 
//    	Mockito.when(bundleRepository.save(bundle)).thenReturn(bundle); 
    	
    	mvc.perform(post("/bundles/1/add/items/1")).andExpect(status().isOk()).andExpect(
//				jsonPath("$.items", hasSize(1))).andExpect( 
				jsonPath("$.items[0].id").value(1)).andExpect(
						jsonPath("$.items[1].id").doesNotExist());    	
    }

    @Test
    public void shouldNotDeleteNonExistingItemFromBundle() throws Exception {   	
    	Item item = new Item();
    	item.setId(1l);
    	item.setBundles(new HashSet<Bundle>());
    	
    	Item itemDummy = new Item();
    	itemDummy.setId(2l);
    	itemDummy.setBundles(new HashSet<Bundle>());
    	
    	Bundle bundle = new Bundle();
    	bundle.setId(1l);
    	bundle.setItems(new HashSet<Item>());
    	
    	item.getBundles().add(bundle);
    	bundle.getItems().add(item);    	
    	
    	Mockito.when(itemRepository.findById(2l)).thenReturn(Optional.of(itemDummy)); 
    	Mockito.when(bundleRepository.findById(1l)).thenReturn(Optional.of(bundle)); 
    	
    	mvc.perform(post("/bundles/1/delete/items/2")).andExpect(status().isOk()).andExpect(
//				jsonPath("$.items", hasSize(1))).andExpect( 
				jsonPath("$.items[0].id").value(1));    	
    }


}