package com.siwimi.webparsers.repository.mongo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.siwimi.webparsers.domain.Activity;
import com.siwimi.webparsers.repository.ActivityRepositoryCustom;

@Repository
public class ActivityRepositoryImpl implements ActivityRepositoryCustom {
	
	@Autowired
	private MongoTemplate mongoTemplate;

	@SuppressWarnings("static-access")
	@Override
	public Activity queryExistedActivity(String creatorId, String title, String description) {			
		List<Criteria> criterias = new ArrayList<Criteria>();
		
		criterias.add(new Criteria().where("isDeletedRecord").is(false));

		if (creatorId != null) {
			criterias.add(new Criteria().where("creator").is(creatorId));
		}
		
		if (title != null) {
			criterias.add(new Criteria().where("title").is(title));
		}
		
		if (description != null) {
			criterias.add(new Criteria().where("description").is(description));
		}
		
		Criteria c = new Criteria().andOperator(criterias.toArray(new Criteria[criterias.size()]));
		
		return mongoTemplate.findOne(new Query(c), Activity.class, "Activity");
				
	}
	
	@SuppressWarnings("static-access")
	@Override
	public Boolean isExisted(String customData, String parser) {
		if (customData == null) {
			return false;
		}
		
		List<Criteria> criterias = new ArrayList<Criteria>();
		
		criterias.add(new Criteria().where("isDeletedRecord").is(false));
		criterias.add(new Criteria().where("customData").is(customData));
		criterias.add(new Criteria().where("parser").is(parser));
		
		Criteria c = new Criteria().andOperator(criterias.toArray(new Criteria[criterias.size()]));
		
		Activity one = mongoTemplate.findOne(new Query(c), Activity.class, "Activity");
		
		return one != null;
	}	
		
	@Override
	public Activity saveActivity(Activity newActivity) {		
		
		// Save new event into mongoDB regardless the image.
		mongoTemplate.indexOps(Activity.class).ensureIndex(new GeospatialIndex("location"));
		mongoTemplate.save(newActivity, "Activity");
	
		// update imageURL
		if (newActivity.getImageData() != null) {			
			// get image path
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			Properties properties = new Properties();
			try {
				properties.load(classLoader.getResourceAsStream("eventImagePath.properties"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// get image type
			String type = ".png";
			String [] parts = newActivity.getImageData().split(",");
			if (parts[0].contains("jpeg"))
				type = ".jpg";
			else if (parts[0].contains("gif"))
				type = ".gif";
			
			// Decode image from String
			byte[] decodedData = Base64.getDecoder().decode(parts[1]);
			
			// Save image to file			
	        if (decodedData != null) {
	            try {
	            	String rootPath = System.getProperty("user.home");
	            	File dir = new File(rootPath + properties.getProperty("path"));
	                if (!dir.exists())
	                    dir.mkdirs();
	                File serverFile = new File(dir.getAbsolutePath() + File.separator + newActivity.getId()+type);
	                
	                BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(serverFile));
	                stream.write(decodedData);
	                stream.close();
	                
	                // update imageURL and save to DB
	                newActivity.setImageUrl(properties.getProperty("path") + newActivity.getId() + type);
	                mongoTemplate.save(newActivity, "Activity");
	            } catch (Exception e) {
	            	e.printStackTrace();
	            }
	        } 
	    }
		
		return newActivity;
	}	
	
}
