package idv.rennnhong.linebot.service.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.maps.GeoApiContext;
import com.google.maps.NearbySearchRequest;
import com.google.maps.PlaceDetailsRequest;
import com.google.maps.PlacesApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.*;
import idv.rennnhong.linebot.map.model.NearByParam;
import idv.rennnhong.linebot.map.model.Place;
import idv.rennnhong.linebot.service.PlaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class PlaceServiceImpl implements PlaceService {

    @Autowired
    private GeoApiContext context;

    @Override
    public List<Place> getNearByPlaces(NearByParam param) {
        List<Place> placeResults = Lists.newArrayList();

        NearbySearchRequest nearbySearchRequest = PlacesApi.nearbySearchQuery(context, new LatLng(param.getLatitude(), param.getLongitude()));
        nearbySearchRequest.radius(param.getRadius());
        nearbySearchRequest.type(PlaceType.CAFE);
        try {
            PlacesSearchResponse placesSearchResponse = nearbySearchRequest.await();
            for (PlacesSearchResult result : placesSearchResponse.results) {
                String placeId = result.placeId;
                PlaceDetailsRequest placeDetailsRequest = PlacesApi.placeDetails(context, placeId);
                PlaceDetails placeDetails = placeDetailsRequest.await();
                placeResults.add(new Place(
                        placeDetails.name,
                        placeDetails.formattedAddress,
                        placeDetails.url.toString(),
                        placeDetails.photos[0].photoReference
                ));
            }
        } catch (ApiException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (placeResults.size() == 0) placeResults = Collections.EMPTY_LIST;

        return placeResults;
    }
}
