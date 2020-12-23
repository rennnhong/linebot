package idv.rennnhong.linebot.service;

import idv.rennnhong.linebot.map.model.NearByParam;
import idv.rennnhong.linebot.map.model.Place;

import java.util.List;

public interface PlaceService {
    List<Place> getNearByPlaces(NearByParam param);

}
