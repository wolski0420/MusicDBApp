package edu.agh.services;

import edu.agh.entities.Artist;
import edu.agh.entities.Category;
import edu.agh.entities.Song;

import java.util.*;

public class SongService extends GenericService<Song>  {

    @Override
    Class<Song> getEntityType() {
        return Song.class;
    }

    // it should work with no artists names given also (empty collection will be given to song constructor)
    public Song addNewSong(final String title, final String categoryName, final String[] artistsNames){
        final Collection<Artist> artists = new ArrayList<>();
        final Collection<String> notCreatedArtists = new ArrayList<>();

        // finding artists in DB
        for(String name : artistsNames){
            Map<String,Object> params = new HashMap<>();
            params.put("artist_name",name);
            String artistQuery = "MATCH (n:Artist{name:$artist_name}) RETURN n";
            Collection<Artist> foundArtists = resultToArtistCollection(executor.query(artistQuery,params));

            if(!foundArtists.isEmpty()){
                artists.add(foundArtists.iterator().next());
            }
            else{
                notCreatedArtists.add(name);
            }
        }

        Category category = Category.fromString(categoryName);   // null when invalid category given (check impl.)

        // creating in DB
        SongService service = new SongService();
        Song song;
        if(category == null) song = new Song(title,artists);
        else song = new Song(title,category,artists);
        service.createOrUpdate(song);
        service.closeSession();

        // creating not existing artists
        if(!notCreatedArtists.isEmpty()){
            for(String name: notCreatedArtists){
                ArtistService artistService = new ArtistService();
                Artist artist = artistService.addNewArtist(name,new String[]{title});
                song.addArtists(Collections.singletonList(artist));
                artistService.closeSession();
            }

            SongService service2 = new SongService();
            service2.createOrUpdate(song);
            service2.closeSession();
        }

        closeSession();
        return song;
    }

    private Collection<Artist> resultToArtistCollection(Iterator<Map<String,Object>> iterator){
        final Collection<Artist> artists = new ArrayList<>();

        while(iterator.hasNext()){
            Map<String,Object> entry = iterator.next();
            entry.forEach((string,object)->artists.add((Artist)object));
        }

        return artists;
    }
}
