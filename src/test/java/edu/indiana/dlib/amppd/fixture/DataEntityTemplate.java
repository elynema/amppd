package edu.indiana.dlib.amppd.fixture;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import br.com.six2six.fixturefactory.loader.TemplateLoader;
import edu.indiana.dlib.amppd.model.Collection;
import edu.indiana.dlib.amppd.model.CollectionSupplement;
import edu.indiana.dlib.amppd.model.Item;
import edu.indiana.dlib.amppd.model.ItemSupplement;
import edu.indiana.dlib.amppd.model.Primaryfile;
import edu.indiana.dlib.amppd.model.PrimaryfileSupplement;
import edu.indiana.dlib.amppd.model.Unit;


//@Component
public class DataEntityTemplate implements TemplateLoader {
	public static String TASK_MANAGER = "Jira|Trello|OpenProject|Redmine";
	public static String EXTERNAL_SOURCE = "Jira|Trello|OpenProject|Redmine";	
	
//	@Autowired
//    private DataentityService dataentityService;	
//	
//	@Autowired
//    private TestHelper testHelper;
	    
	@Override
	public void load() {
//		Random rand = new Random();
//		Long id = 0L;
//		String[] taskManagers = dataentityService.getAllowedExternalSources();
//		String[] externalSources = dataentityService.getAllowedExternalSources();
//		
//		Unit unit = testHelper.ensureUnit("Test Unit");
//		Collection collection = testHelper.ensureCollection("Test Unit", "Test Collection");
//		Item item = testHelper.ensureItem("Test Unit", "Test Collection", "Test Item");
//		Primaryfile primaryfile = testHelper.ensurePrimaryfile("Test Unit", "Test Collection", "Test Item", "Test Primaryfile");
//		String unitUrl = dataentityService.getDataentityUrl(unit);
//		String collectionUrl = dataentityService.getDataentityUrl(collection);
//		String itemUrl = dataentityService.getDataentityUrl(item);
//		String primaryfileUrl = dataentityService.getDataentityUrl(primaryfile);		
		
		Fixture.of(Unit.class).addTemplate("valid", new Rule() {{ 
			add("id", random(Long.class));
			add("name", "Test Unit ${id}");
			add("description", "Description for ${name}");	
		}});

		Fixture.of(Unit.class).addTemplate("invalid", new Rule() {{ 
			add("name", "");
		}});

		Fixture.of(Collection.class).addTemplate("valid", new Rule() {{
			add("id", random(Long.class));
			add("name", "Test Collection ${id}");
			add("description", "Description for ${name}");	
			add("externalSource", regex(EXTERNAL_SOURCE));
			add("externalId", random(Integer.class));
			add("taskManager", regex(TASK_MANAGER));
//			add("externalSource", externalSources[rand.nextInt(externalSources.length)]);
//			add("taskManager", taskManagers[rand.nextInt(taskManagers.length)]);
//			add("unit", unitUrl);
		}}); 
			
		Fixture.of(Collection.class).addTemplate("invalid", new Rule() {{
			add("name", "");
			add("externalSource", "FakeExternalSource");
			add("taskManager", "FakeTaskManager");
		}}); 

		Fixture.of(Item.class).addTemplate("valid", new Rule() {{			
			add("id", random(Long.class));
			add("name", "Test Item ${id}");
			add("description", "Description for ${name}");	
			add("externalSource", regex(EXTERNAL_SOURCE));
			add("externalId", random(Integer.class));
//			add("collection", collectionUrl);
		}});

		Fixture.of(Item.class).addTemplate("invalid", new Rule() {{			
			add("name", "");
			add("externalSource", "FakeExternalSource");
		}});

		Fixture.of(Primaryfile.class).addTemplate("valid", new Rule() {{
			add("id", random(Long.class));
			add("name", "Test Primaryfile ${id}");
			add("description", "Description for ${name}");	
//			add("item", itemUrl);
//			add("originalFilename", firstName());
//			add("pathname", "C:/New Folder/${name}");
//			add("mediaInfo", "{}");
		}});

		Fixture.of(Primaryfile.class).addTemplate("invalid", new Rule() {{ 
			add("name", "");
		}});
		
		Fixture.of(CollectionSupplement.class).addTemplate("valid", new Rule() {{
			add("id", random(Long.class));
			add("name", "Test CollectionSupplement ${id}");
			add("description", "Description for ${name}");	
//			add("collection", collectionUrl);
//			add("originalFilename", firstName());
//			add("pathname", "C:/New Folder/${name}");
//			add("mediaInfo", "{}");
		}});

		Fixture.of(CollectionSupplement.class).addTemplate("invalid", new Rule() {{
			add("name", "");
		}}); 
		
		Fixture.of(ItemSupplement.class).addTemplate("valid", new Rule() {{
			add("id", random(Long.class));
			add("name", "Test ItemSupplement ${id}");
			add("description", "Description for ${name}");	
//			add("item", itemUrl);
//			add("originalFilename", firstName());
//			add("pathname", "C:/New Folder/${name}");
//			add("mediaInfo", "{}");
		}});

		Fixture.of(ItemSupplement.class).addTemplate("invalid", new Rule() {{
			add("name", "");
		}}); 
				
		Fixture.of(PrimaryfileSupplement.class).addTemplate("valid", new Rule() {{
			add("id", random(Long.class));
			add("name", "PrimaryfileSupplement ${id}");
			add("description", "Description for ${name}");	
//			add("primaryfile", primaryfileUrl);
//			add("originalFilename", firstName());
//			add("pathname", "C:/New Folder/${name}");
//			add("mediaInfo", "{}");
		}});
		
		Fixture.of(PrimaryfileSupplement.class).addTemplate("invalid", new Rule() {{
			add("name", "");
		}}); 				
	}

}
