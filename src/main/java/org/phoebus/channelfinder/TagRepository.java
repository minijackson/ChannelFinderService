package org.phoebus.channelfinder;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch._types.*;
import org.phoebus.channelfinder.XmlTag.OnlyXmlTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch.core.ExistsRequest;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest.Builder;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.util.ObjectBuilder;

@Repository
@Configuration
public class TagRepository implements CrudRepository<XmlTag, String> {
    static Logger log = Logger.getLogger(TagRepository.class.getName());

    @Value("${elasticsearch.tag.index:cf_tags}")
    private String ES_TAG_INDEX;
    @Value("${elasticsearch.tag.type:cf_tag}")
    private String ES_TAG_TYPE;

    @Autowired
    @Qualifier("indexClient")
    ElasticsearchClient client;

    @Autowired
    ChannelRepository channelRepository;

    ObjectMapper objectMapper = new ObjectMapper().addMixIn(XmlTag.class, OnlyXmlTag.class);

    /**
     * create a new tag using the given XmlTag
     * 
     * @param <S> extends XmlTag
     * @param tag - tag to be created
     * @return the created tag
     */
    @SuppressWarnings("unchecked")
    public <S extends XmlTag> S index(S tag) {
        try {
            IndexRequest request = IndexRequest.of(i -> i.index(ES_TAG_INDEX).id(tag.getName()).document(tag).refresh(Refresh.True));

            IndexResponse response = client.index(request);
            /// verify the creation of the tag
            if (response.result().equals(Result.Created) || response.result().equals(Result.Updated)) {
                log.config("Created tag " + tag);
                return tag;
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to index tag " + tag.toLog(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to index tag: " + tag, null);
        }
        return null;
    }

    /**
     * create new tags using the given XmlTags
     * 
     * @param <S>  extends XmlTag
     * @param tags - tags to be created
     * @return the created tags
     */
    @SuppressWarnings("unchecked")
    public <S extends XmlTag> Iterable<S> indexAll(Iterable<S> tags) {
        
//        RestHighLevelClient client = esService.getNewClient();
//        try {
//            BulkRequest bulkRequest = new BulkRequest();
//            for (XmlTag tag : tags) {
//                IndexRequest indexRequest = new IndexRequest(ES_TAG_INDEX, ES_TAG_TYPE).id(tag.getName())
//                        .source(objectMapper.writeValueAsBytes(tag), XContentType.JSON);
//                bulkRequest.add(indexRequest);
//            }
//
//            bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
//            /// verify the creation of the tags
//            if (bulkResponse.hasFailures()) {
//                // Failed to create all the tags
//            } else {
//                List<String> createdTagIds = new ArrayList<String>();
//                for (BulkItemResponse bulkItemResponse : bulkResponse) {
//                    Result result = bulkItemResponse.getResponse().getResult();
//                    if (result.equals(Result.CREATED) || result.equals(Result.UPDATED)) {
//                        createdTagIds.add(bulkItemResponse.getId());
//                    }
//                }
//                return (Iterable<S>) findAllById(createdTagIds);
//            }
//        } catch (Exception e) {
//            log.log(Level.SEVERE, "Failed to index tags " + tags, e);
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to index tags: " + tags, null);
//        }
        return null;
    }

    /**
     * update/save tag using the given XmlTag
     * 
     * @param <S>     extends XmlTag
     * @param tagName - name of tag to be created
     * @param tag     - tag to be created
     * @return the updated/saved tag
     */
    @SuppressWarnings("unchecked")
    public <S extends XmlTag> S save(String tagName, S tag) {
        // Cleanup old tag instance if present
        boolean existingTag = existsById(tagName);
        if (existingTag) {
            deleteById(tagName);
        }

        try {
            IndexResponse response = client
                    .index(i -> i.index(ES_TAG_INDEX).id(tagName).document(tag).refresh(Refresh.True));
            // return response.result().;
            // TODO return the result of the query
            return tag;
        } catch (ElasticsearchException | IOException e) {
            log.log(Level.SEVERE, "Failed to update/save tag:" + tag.toLog(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update/save tag: " + tag,
                    null);
        }

//        RestHighLevelClient client = esService.getNewClient();
//        try {
//            UpdateRequest updateRequest;
//            Optional<XmlTag> existingTag = findById(tagName);
//            boolean present = existingTag.isPresent();
//            if (present) {
//                deleteById(tagName);
//            }
//            updateRequest = new UpdateRequest(ES_TAG_INDEX, ES_TAG_TYPE, tag.getName());
//            IndexRequest indexRequest = new IndexRequest(ES_TAG_INDEX, ES_TAG_TYPE).id(tag.getName())
//                    .source(objectMapper.writeValueAsBytes(tag), XContentType.JSON);
//            updateRequest.doc(objectMapper.writeValueAsBytes(tag), XContentType.JSON).upsert(indexRequest);
//            updateRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//            UpdateResponse updateResponse = client.update(updateRequest, RequestOptions.DEFAULT);
//            /// verify the updating/saving of the tag
//            Result result = updateResponse.getResult();
//            if (result.equals(Result.CREATED) || result.equals(Result.UPDATED) || result.equals(Result.NOOP)) {
//                // client.get(, options)
//                return (S) findById(tag.getName()).get();
//            }
//        } catch (Exception e) {
//            log.log(Level.SEVERE, "Failed to update/save tag:" + tag.toLog(), e);
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update/save tag: " + tag,
//                    null);
//        }
    }

    @Override
    public <S extends XmlTag> S save(S tag) {
        return save(tag.getName(), tag);
    }

    /**
     * update/save tags using the given XmlTags
     * 
     * @param <S>  extends XmlTag
     * @param tags - tags to be created
     * @return the updated/saved tags
     */
    @SuppressWarnings("unchecked")
    @Override
    public <S extends XmlTag> Iterable<S> saveAll(Iterable<S> tags) {
//        RestHighLevelClient client = esService.getNewClient();
//        BulkRequest bulkRequest = new BulkRequest();
//        try {
//            for (XmlTag tag : tags) {
//                UpdateRequest updateRequest = new UpdateRequest(ES_TAG_INDEX, ES_TAG_TYPE, tag.getName());
//
//                Optional<XmlTag> existingTag = findById(tag.getName());
//                if (existingTag.isPresent()) {
//                    updateRequest.doc(objectMapper.writeValueAsBytes(tag), XContentType.JSON);
//                } else {
//                    IndexRequest indexRequest = new IndexRequest(ES_TAG_INDEX, ES_TAG_TYPE).id(tag.getName())
//                            .source(objectMapper.writeValueAsBytes(tag), XContentType.JSON);
//                    updateRequest.doc(objectMapper.writeValueAsBytes(tag), XContentType.JSON).upsert(indexRequest);
//                }
//                bulkRequest.add(updateRequest);
//            }
//
//            bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
//            if (bulkResponse.hasFailures()) {
//                // Failed to create/update all the tags
//                throw new Exception();
//            } else {
//                List<String> createdTagIds = new ArrayList<String>();
//                for (BulkItemResponse bulkItemResponse : bulkResponse) {
//                    Result result = bulkItemResponse.getResponse().getResult();
//                    if (result.equals(Result.CREATED) || result.equals(Result.UPDATED) || result.equals(Result.NOOP)) {
//                        createdTagIds.add(bulkItemResponse.getId());
//                    }
//                }
//                return (Iterable<S>) findAllById(createdTagIds);
//            }
//        } catch (Exception e) {
//            log.log(Level.SEVERE, "Failed to update/save tags" + tags, e);
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update/save tags: " + tags,
//                    null);
//        }
        return null;
    }

    /**
     * find tag using the given tag id
     * 
     * @param tagId - id of tag to be found
     * @return the found tag
     */
    @Override
    public Optional<XmlTag> findById(String tagId) {
        return findById(tagId, false);
    }

    /**
     * find tag using the given tag id
     * 
     * @param tagId        - id of tag to be found
     * @param withChannels - whether channels should be included
     * @return the found tag
     */
    public Optional<XmlTag> findById(String tagId, boolean withChannels) {
        GetResponse<XmlTag> response;
        try {
            response = client.get(g -> g.index(ES_TAG_INDEX).id(tagId), XmlTag.class);

            if (response.found()) {
                XmlTag tag = response.source();
                log.info("Tag name " + tag.getName());
                // TODO if (withChannels)
                return Optional.of(tag);
            } else {
                log.info("Tag not found");
                return Optional.empty();
            }
        } catch (ElasticsearchException | IOException e) {
            log.log(Level.SEVERE, "Failed to find tag " + tagId, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find tag: " + tagId, null);
        }
    }

    @Override
    public boolean existsById(String id) {
        try {
            ExistsRequest.Builder builder = new ExistsRequest.Builder();
            builder.index(ES_TAG_INDEX).id(id);
            return client.exists(builder.build()).value();
        } catch (ElasticsearchException | IOException e) {
            log.log(Level.SEVERE, "Failed to check if tag " + id + " exists", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to check if tag exists by id: " + id, null);
        }
    }

    /**
     * find all tags
     * 
     * @return the found tags
     */
    @Override
    public Iterable<XmlTag> findAll() {
        try {
            SearchRequest.Builder searchBuilder = new Builder()
                    .query(new MatchAllQuery.Builder().build()._toQuery())
                    .size(10000)
                    .sort(SortOptions.of(s -> s.field(FieldSort.of(f -> f.field("name")))));
            SearchResponse<XmlTag> response = client.search(searchBuilder.build(), XmlTag.class);
            return response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        } catch (ElasticsearchException | IOException e) {
            log.log(Level.SEVERE, "Failed to find all tags", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find all tags", null);
        }
    }

    /**
     * find tags using the given tags ids
     * 
     * @param tagIds - ids of tags to be found
     * @return the found tags
     */
    @Override
    public Iterable<XmlTag> findAllById(Iterable<String> tagIds) {
//        MultiGetRequest request = new MultiGetRequest();
//
//        for (String tagId : tagIds) {
//            request.add(new MultiGetRequest.Item(ES_TAG_INDEX, ES_TAG_TYPE, tagId));
//        }
//        try {
//            List<XmlTag> foundTags = new ArrayList<XmlTag>();
//            MultiGetResponse response = esService.getSearchClient().mget(request, RequestOptions.DEFAULT);
//            for (MultiGetItemResponse multiGetItemResponse : response) {
//                if (!multiGetItemResponse.isFailed()) {
//                    foundTags.add(objectMapper.readValue(
//                            multiGetItemResponse.getResponse().getSourceAsBytesRef().streamInput(), XmlTag.class));
//                }
//            }
//            return foundTags;
//        } catch (Exception e) {
//            log.log(Level.SEVERE, "Failed to find tags: " + tagIds, e);
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find tags: " + tagIds, null);
//        }
        return null;
    }

    @Override
    public long count() {
        // NOT USED
        return 0;
    }

    /**
     * delete the given tag by tag name
     * 
     * @param tagName - tag to be deleted
     */
    @Override
    public void deleteById(String tagName) {
//        RestHighLevelClient client = esService.getNewClient();
//        DeleteRequest request = new DeleteRequest(ES_TAG_INDEX, ES_TAG_TYPE, tagName);
//        request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//
//        try {
//            DeleteResponse response = client.delete(request, RequestOptions.DEFAULT);
//            Result result = response.getResult();
//            if (!result.equals(Result.DELETED)) {
//                throw new Exception();
//            }
//            // delete tag from channels
//            MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
//            params.add("~tag", tagName);
//            List<XmlChannel> chans = channelRepository.search(params);
//            if (!chans.isEmpty()) {
//                chans.forEach(chan -> chan.removeTag(new XmlTag(tagName, "")));
//                channelRepository.indexAll(chans);
//            }
//        } catch (Exception e) {
//            log.log(Level.SEVERE, "Failed to delete tag: " + tagName, e);
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete tag: " + tagName,
//                    null);
//        }

    }

    /**
     * delete the given tag
     * 
     * @param tag - tag to be deleted
     */
    @Override
    public void delete(XmlTag tag) {
        deleteById(tag.getName());
    }

    @Override
    public void deleteAll(Iterable<? extends XmlTag> entities) {
        throw new UnsupportedOperationException("Delete All is not supported.");
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException("Delete All is not supported.");
    }

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        // TODO Auto-generated method stub

    }
}
