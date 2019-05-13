package edu.indiana.dlib.amppd.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import edu.indiana.dlib.amppd.exception.StorageException;
import edu.indiana.dlib.amppd.model.CollectionSupplement;
import edu.indiana.dlib.amppd.model.ItemSupplement;
import edu.indiana.dlib.amppd.model.Primaryfile;
import edu.indiana.dlib.amppd.model.PrimaryfileSupplement;
import edu.indiana.dlib.amppd.repository.CollectionSupplementRepository;
import edu.indiana.dlib.amppd.repository.ItemSupplementRepository;
import edu.indiana.dlib.amppd.repository.PrimaryfileRepository;
import edu.indiana.dlib.amppd.repository.PrimaryfileSupplementRepository;
import edu.indiana.dlib.amppd.service.FileStorageService;
import edu.indiana.dlib.amppd.service.impl.FileStorageServiceImpl;
import lombok.extern.java.Log;

// TODO: when we add controllers for data entities, we might want to move the actions into controllers for the associated entities.

/**
 * Controller to handle file upload for primaryfiles and supplements.
 * @author yingfeng
 *
 */
@RestController
@Log
public class FileUploadController {
	
	@Autowired
    private FileStorageService fileStorageService;
	
	@Autowired
    private PrimaryfileRepository primaryfileRepository;
	
	@Autowired
    private CollectionSupplementRepository collectionSupplementRepository;

	@Autowired
    private ItemSupplementRepository itemSupplementRepository;

	@Autowired
    private PrimaryfileSupplementRepository primaryfileSupplementRepository;

	// TODO: handle redirect for all following methods
	// TODO: consider moving most logic in methods into FileStorageServiceImpl
	
	@PostMapping("/primaryfiles/{id}/file")
    public String handlePrimaryfileUpload(@PathVariable("id") Long id, @RequestParam("file") MultipartFile file, HttpServletResponse response, RedirectAttributes redirectAttributes) {		
    	Primaryfile primaryfile = primaryfileRepository.findById(id).orElseThrow(() -> new StorageException("Primaryfile <" + id + "> does not exist!"));    
    	primaryfile.setOriginalFilename(StringUtils.cleanPath(file.getOriginalFilename()));	
    	String targetPathname = fileStorageService.getFilePathname(primaryfile);    	    	
    	primaryfile.setPathname(targetPathname);
    	fileStorageService.store(file, targetPathname);    	
    	primaryfileRepository.save(primaryfile);  
    	
    	String msg = "You successfully uploaded primaryfile " + file.getOriginalFilename() + " to " + targetPathname + "!";
    	log.info(msg);
        redirectAttributes.addFlashAttribute("message", msg);
//        try {
//        	response.sendRedirect("/primaryfiles/" + id + "/files");
//        }
//        catch (IOException e) {
//        	throw new RuntimeException("Cannot redirect to after uploading file.", e);
//        }
        return "redirect:/";
    }

    @PostMapping("/collections/supplements/{id}/file")
    public String handleCollectionSupplementUpload(@PathVariable("id") Long id, @RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {		
    	CollectionSupplement collectionSupplement = collectionSupplementRepository.findById(id).orElseThrow(() -> new StorageException("CollectionSupplement <" + id + "> does not exist!"));
    	collectionSupplement.setOriginalFilename(StringUtils.cleanPath(file.getOriginalFilename()));
    	String targetPathname = fileStorageService.getFilePathname(collectionSupplement);    		    	
    	collectionSupplement.setPathname(targetPathname);
    	fileStorageService.store(file, targetPathname);
    	collectionSupplementRepository.save(collectionSupplement);   	
	
    	String msg = "You successfully uploaded collectionSupplement " + file.getOriginalFilename() + " to " + targetPathname + "!";
    	log.info(msg);
        redirectAttributes.addFlashAttribute("message", msg);
        return "redirect:/";
    }
    
    @PostMapping("/items/supplements/{id}/file")
    public String handleItemSupplementUpload(@PathVariable("id") Long id, @RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {		
    	ItemSupplement itemSupplement = itemSupplementRepository.findById(id).orElseThrow(() -> new StorageException("itemSupplement <" + id + "> does not exist!"));
    	itemSupplement.setOriginalFilename(StringUtils.cleanPath(file.getOriginalFilename()));
    	String targetPathname = fileStorageService.getFilePathname(itemSupplement);    		    	
    	itemSupplement.setPathname(targetPathname);
    	fileStorageService.store(file, targetPathname);
    	itemSupplementRepository.save(itemSupplement);   	
	    	
    	String msg = "You successfully uploaded itemSupplement " + file.getOriginalFilename() + " to " + targetPathname + "!";
    	log.info(msg);
        redirectAttributes.addFlashAttribute("message", msg);
        return "redirect:/";
    }
    
    @PostMapping("/primaryfiles/supplements/{id}/file")
    public String handlePrimaryfileSupplementUpload(@PathVariable("id") Long id, @RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {		
    	PrimaryfileSupplement primaryfileSupplement = primaryfileSupplementRepository.findById(id).orElseThrow(() -> new StorageException("primaryfileSupplement <" + id + "> does not exist!"));
    	primaryfileSupplement.setOriginalFilename(StringUtils.cleanPath(file.getOriginalFilename()));
    	String targetPathname = fileStorageService.getFilePathname(primaryfileSupplement);    		    	
    	primaryfileSupplement.setPathname(targetPathname);
    	fileStorageService.store(file, targetPathname);
    	primaryfileSupplementRepository.save(primaryfileSupplement);   	

    	String msg = "You successfully uploaded primaryfileSupplement " + file.getOriginalFilename() + " to " + targetPathname + "!";
    	log.info(msg);
    	redirectAttributes.addFlashAttribute("message", msg);
        return "redirect:/";
    }
        
    
//  @GetMapping("/")
//  public String listUploadedFiles(Model model) throws IOException {
//
//      model.addAttribute("files", fileStorageService.loadAll().map(
//              path -> MvcUriComponentsBuilder.fromMethodName(PrimaryController.class,
//                      "serveFile", path.getFilename().toString()).build().toString())
//              .collect(Collectors.toList()));
//
//      return "uploadForm";
//  }

//  @GetMapping("/files/{filename:.+}")
//  @ResponseBody
//  public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
//
//      Resource file = fileStorageService.loadAsResource(filename);
//      return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
//              "attachment; filename=\"" + file.getFilename() + "\"").body(file);
//  }    

//    @ExceptionHandler(StorageFileNotFoundException.class)
//    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
//        return ResponseEntity.notFound().build();
//    }

}